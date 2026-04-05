package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.asset.services.VStorageIndexItemWithAssetService;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.CodeInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class PdfActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "pdf";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_FILE_NAME = "fileName";
    private static final String OUTPUT_NAME_MIME_TYPE = "mimeType";
    private static final String OUTPUT_NAME_SIZE_BYTES = "sizeBytes";
    private static final String OUTPUT_NAME_ATTACHMENT_KEY = "attachmentKey";
    private static final String OUTPUT_NAME_STORAGE_PROVIDER_ID = "storageProviderId";
    private static final String OUTPUT_NAME_STORAGE_PATH_FROM_ROOT = "storagePathFromRoot";
    private static final String HEADER_HTML_MARKER = "::header::";
    private static final String FOOTER_HTML_MARKER = "::footer::";
    private static final Pattern HTML_DOCUMENT_BLOCK_PATTERN = Pattern.compile(
            "<html\\b.*?</html>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final PdfService pdfService;
    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final AssetService assetService;
    private final VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository;
    private final StorageService storageService;

    public PdfActionNodeV1(PdfService pdfService,
                           TemplateRenderService templateRenderService,
                           ProcessInstanceAttachmentService processInstanceAttachmentService,
                           AssetService assetService,
                           VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository,
                           StorageService storageService) {
        this.pdfService = pdfService;
        this.templateRenderService = templateRenderService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.assetService = assetService;
        this.vStorageIndexItemWithAssetRepository = vStorageIndexItemWithAssetRepository;
        this.storageService = storageService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return NODE_KEY;
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public String getName() {
        return "PDF erstellen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erzeugt ein PDF-Dokument aus HTML-Inhalt über Gotenberg.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(PdfActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für die PDF-Erstellung: %s",
                    e.getMessage()
            );
        }

        layout
                .findChild(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_ID, RadioInputElement.class)
                .ifPresent(element -> element.setOptions(List.of(
                        RadioInputElementOption.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE, "Manuelle Eingabe (HTML)"),
                        RadioInputElementOption.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY, "PDF-Vorlage (Dateien & Medien)")
                )));

        layout
                .findChild(PdfActionNodeConfig.CONTENT_HTML_CODE_FIELD_ID, CodeInputElement.class)
                .ifPresent(element -> {
                    element.setVisibility(ElementVisibilityFunctions.of(
                                    NoCodeExpression.of(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            NoCodeReference.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_ID),
                                            NoCodeStaticValue.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE)
                                    )
                            )
                            .recalculateReferencedIds());
                });

        layout
                .findChild(PdfActionNodeConfig.CONTENT_HTML_ASSET_KEY_FIELD_ID, SelectInputElement.class)
                .ifPresent(element -> {
                    element.setVisibility(ElementVisibilityFunctions.of(
                                    NoCodeExpression.of(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            NoCodeReference.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_ID),
                                            NoCodeStaticValue.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY)
                                    )
                            )
                            .recalculateReferencedIds());

                    element.setOptions(vStorageIndexItemWithAssetRepository
                            .findAllByMimeType("text/html")
                            .stream()
                            .map((ass) -> SelectInputElementOption.of(
                                    ass.getAssetKey().toString(),
                                    ass.getPathFromRoot()
                            ))
                            .toList());
                });

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Weiter",
                        "Der Prozess wird hier fortgesetzt, nachdem das PDF erzeugt wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_FILE_NAME,
                        "Dateiname",
                        "Der Dateiname des erzeugten PDF-Dokuments."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_MIME_TYPE,
                        "MIME-Typ",
                        "Der MIME-Typ des erzeugten Dokuments."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ATTACHMENT_KEY,
                        "Anhangs-Schlüssel",
                        "Der Schlüssel des erzeugten Prozess-Anhangs."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_STORAGE_PROVIDER_ID,
                        "Speicheranbieter-ID",
                        "Die ID des Speicheranbieters des erzeugten Prozess-Anhangs."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_STORAGE_PATH_FROM_ROOT,
                        "Speicherpfad",
                        "Der Pfad zum erzeugten Prozess-Anhang im Speicheranbieter."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_SIZE_BYTES,
                        "Dateigroesse in Bytes",
                        "Die Groesse des erzeugten PDF-Dokuments in Bytes."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        PdfActionNodeConfig configuration;
        try {
            configuration = ElementPOJOMapper
                    .mapToPOJO(context.getConfiguration().getEffectiveValues(), PdfActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des PDF-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        var fileName = templateRenderService
                .interpolate(context.getProcessData(), configuration.fileName);
        if (StringUtils.isNullOrEmpty(fileName)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Dateiname für das PDF wurde nicht angegeben."
            );
        }
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName += ".pdf";
        }

        var contentHtml = resolveContentHtml(context, configuration);
        if (StringUtils.isNullOrEmpty(contentHtml)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der HTML-Inhalt für das PDF wurde nicht angegeben."
            );
        }

        var pdfHtmlSections = splitHtmlSections(contentHtml);
        var interpolatedContentHtml = templateRenderService
                .interpolate(context.getProcessData(), pdfHtmlSections.contentHtml);

        if (StringUtils.isNullOrEmpty(interpolatedContentHtml)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der HTML-Inhalt für das PDF konnte nicht interpoliert werden."
            );
        }

        var interpolatedHeaderHtml = templateRenderService
                .interpolate(context.getProcessData(), pdfHtmlSections.headerHtml);
        var interpolatedFooterHtml = templateRenderService
                .interpolate(context.getProcessData(), pdfHtmlSections.footerHtml);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdfFromHtml(
                    interpolatedContentHtml,
                    interpolatedHeaderHtml,
                    interpolatedFooterHtml
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die PDF-Erstellung wurde unterbrochen."
            );
        } catch (URISyntaxException | IOException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Erzeugen des PDFs mit Gotenberg: %s",
                    e.getMessage()
            );
        }

        ProcessInstanceAttachmentEntity attachment;
        try {
            attachment = processInstanceAttachmentService.create(
                    ProcessInstanceAttachmentEntity.of(
                            fileName,
                            context.getThisProcessInstance().getId(),
                            context.getThisTask().getId(),
                            pdfBytes
                    )
            );
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Speichern des erzeugten PDFs als Prozess-Anhang: %s",
                    e.getMessage()
            );
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_FILE_NAME, fileName);
        metadata.put(OUTPUT_NAME_MIME_TYPE, "application/pdf");
        metadata.put(OUTPUT_NAME_SIZE_BYTES, pdfBytes.length);
        metadata.put(OUTPUT_NAME_ATTACHMENT_KEY, attachment.getKey());
        metadata.put(OUTPUT_NAME_STORAGE_PROVIDER_ID, attachment.getStorageProviderId());
        metadata.put(OUTPUT_NAME_STORAGE_PATH_FROM_ROOT, attachment.getStoragePathFromRoot());

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    @Nonnull
    private String resolveContentHtml(@Nonnull ProcessNodeExecutionContextInit context,
                                      @Nonnull PdfActionNodeConfig configuration) throws ProcessNodeExecutionException {
        var contentSource = configuration.contentHtmlSource;
        if (StringUtils.isNullOrEmpty(contentSource)) {
            contentSource = PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE;
        }

        if (PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE.equals(contentSource)) {
            return templateRenderService
                    .interpolate(context.getProcessData(), configuration.contentHtml);
        }

        if (PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY.equals(contentSource)) {
            var assetKeyStr = templateRenderService
                    .interpolate(context.getProcessData(), configuration.contentHtmlAssetKey);
            if (StringUtils.isNullOrEmpty(assetKeyStr)) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der Asset-Schluessel für die PDF-Vorlage wurde nicht angegeben."
                );
            }

            UUID assetKey;
            try {
                assetKey = UUID.fromString(assetKeyStr.trim());
            } catch (IllegalArgumentException e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Der Asset-Schluessel für die PDF-Vorlage ist ungueltig: %s",
                        assetKeyStr
                );
            }

            final var asset = retrieveAsset(assetKey);
            var assetTemplate = loadAssetContentAsString(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
            // Render the full asset template before splitting the individual HTML documents so shared
            // blocks defined outside a specific <html> section remain available to all use sites.
            return templateRenderService.interpolate(context.getProcessData(), assetTemplate);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Die HTML-Quellkonfiguration %s ist unbekannt.",
                StringUtils.quote(contentSource)
        );
    }

    @Nonnull
    private PdfHtmlSections splitHtmlSections(@Nonnull String resolvedHtml) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var matcher = HTML_DOCUMENT_BLOCK_PATTERN.matcher(resolvedHtml);
        if (!matcher.find()) {
            return new PdfHtmlSections(resolvedHtml, "", "");
        }

        matcher.reset();

        String contentHtml = null;
        String headerHtml = "";
        String footerHtml = "";

        while (matcher.find()) {
            var htmlBlock = matcher.group();
            var isHeaderBlock = htmlBlock.contains(HEADER_HTML_MARKER);
            var isFooterBlock = htmlBlock.contains(FOOTER_HTML_MARKER);

            if (isHeaderBlock && isFooterBlock) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Ein HTML-Block der PDF-Vorlage darf nicht gleichzeitig %s und %s enthalten.",
                        StringUtils.quote(HEADER_HTML_MARKER),
                        StringUtils.quote(FOOTER_HTML_MARKER)
                );
            }

            var sanitizedHtmlBlock = sanitizeHtmlBlock(htmlBlock);

            if (isHeaderBlock) {
                if (StringUtils.isNotNullOrEmpty(headerHtml)) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Die PDF-Vorlage enthält mehrere Header-HTML-Blöcke mit %s.",
                            StringUtils.quote(HEADER_HTML_MARKER)
                    );
                }
                headerHtml = sanitizedHtmlBlock;
                continue;
            }

            if (isFooterBlock) {
                if (StringUtils.isNotNullOrEmpty(footerHtml)) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Die PDF-Vorlage enthält mehrere Footer-HTML-Blöcke mit %s.",
                            StringUtils.quote(FOOTER_HTML_MARKER)
                    );
                }
                footerHtml = sanitizedHtmlBlock;
                continue;
            }

            if (StringUtils.isNotNullOrEmpty(contentHtml)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die PDF-Vorlage enthält mehrere HTML-Blöcke ohne Marker für den Dokumentinhalt."
                );
            }

            contentHtml = sanitizedHtmlBlock;
        }

        if (StringUtils.isNullOrEmpty(contentHtml)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die PDF-Vorlage enthält keinen HTML-Block für den Dokumentinhalt."
            );
        }

        return new PdfHtmlSections(contentHtml, headerHtml, footerHtml);
    }

    @Nonnull
    private String sanitizeHtmlBlock(@Nonnull String htmlBlock) {
        return htmlBlock
                .replace(HEADER_HTML_MARKER, "")
                .replace(FOOTER_HTML_MARKER, "");
    }

    @Nonnull
    private de.aivot.GoverBackend.asset.entities.AssetEntity retrieveAsset(@Nonnull UUID assetKey) throws ProcessNodeExecutionException {
        try {
            return assetService
                    .retrieve(assetKey)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionMissingValue(
                            "Die PDF-Vorlage mit dem Schluessel %s wurde nicht gefunden.",
                            assetKey.toString()
                    ));
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die PDF-Vorlage konnte nicht geladen werden: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private String loadAssetContentAsString(@Nonnull Integer storageProviderId,
                                            @Nonnull String storagePathFromRoot) throws ProcessNodeExecutionException {
        try (var content = storageService.getDocumentContent(storageProviderId, storagePathFromRoot)) {
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        } catch (ResponseException | IOException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der Inhalt der PDF-Vorlage konnte nicht geladen werden: %s",
                    e.getMessage()
            );
        }
    }

    private static final class PdfHtmlSections {
        private final String contentHtml;
        private final String headerHtml;
        private final String footerHtml;

        private PdfHtmlSections(@Nonnull String contentHtml,
                                @Nonnull String headerHtml,
                                @Nonnull String footerHtml) {
            this.contentHtml = contentHtml;
            this.headerHtml = headerHtml;
            this.footerHtml = footerHtml;
        }
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class PdfActionNodeConfig {
        public static final String FILE_NAME_FIELD_ID = "file_name";
        public static final String CONTENT_HTML_SOURCE_FIELD_ID = "content_html_source";
        public static final String CONTENT_HTML_SOURCE_FIELD_OPTION_CODE = "contentHtml";
        public static final String CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY = "contentHtmlAssetKey";
        public static final String CONTENT_HTML_CODE_FIELD_ID = "content_html_code";
        public static final String CONTENT_HTML_ASSET_KEY_FIELD_ID = "content_html_asset_key";

        @InputElementPOJOBinding(id = FILE_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateiname"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Dateiname des PDF-Dokuments (z.B. dokument.pdf)."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String fileName;

        @InputElementPOJOBinding(id = CONTENT_HTML_SOURCE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Quelle für die PDF-Vorlage"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Quelle für den HTML-Inhalt der PDF-Seiten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String contentHtmlSource;

        @InputElementPOJOBinding(id = CONTENT_HTML_CODE_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "HTML-Inhalt"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "HTML-Template für die PDF-Seiten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "language", strValue = "html")
        })
        public String contentHtml;

        @InputElementPOJOBinding(id = CONTENT_HTML_ASSET_KEY_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "PDF-Vorlage"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie eine zuvor hochgeladene PDF-Vorlage aus."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String contentHtmlAssetKey;
    }
}
