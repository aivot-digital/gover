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
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
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

    private final PdfService pdfService;
    private final ProcessDataService processDataService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final AssetService assetService;
    private final VStorageIndexItemWithAssetService vStorageIndexItemWithAssetService;
    private final VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository;
    private final StorageService storageService;

    public PdfActionNodeV1(PdfService pdfService,
                           ProcessDataService processDataService,
                           ProcessInstanceAttachmentService processInstanceAttachmentService,
                           AssetService assetService,
                           VStorageIndexItemWithAssetService vStorageIndexItemWithAssetService,
                           VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository,
                           StorageService storageService) {
        this.pdfService = pdfService;
        this.processDataService = processDataService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.assetService = assetService;
        this.vStorageIndexItemWithAssetService = vStorageIndexItemWithAssetService;
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
                            .map((ass) -> RadioInputElementOption.of(
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
                    .mapToPOJO(context.getThisNode().getConfiguration(), PdfActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des PDF-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        var fileName = processDataService
                .interpolate(context.getProcessData(), configuration.fileName);
        if (StringUtils.isNullOrEmpty(fileName)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Dateiname fuer das PDF wurde nicht angegeben."
            );
        }
        if (!fileName.toLowerCase().endsWith(".pdf")) {
            fileName += ".pdf";
        }

        var contentHtml = resolveContentHtml(context, configuration);
        if (StringUtils.isNullOrEmpty(contentHtml)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der HTML-Inhalt fuer das PDF wurde nicht angegeben."
            );
        }

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdfFromHtml(
                    contentHtml,
                    "",
                    "",
                    "210mm",
                    "297mm"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Die PDF-Erstellung wurde unterbrochen."
            );
        } catch (URISyntaxException | java.io.IOException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Erzeugen des PDFs mit Gotenberg: %s",
                    e.getMessage()
            );
        }

        ProcessInstanceAttachmentEntity attachment;
        try {
            attachment = processInstanceAttachmentService.create(
                    new ProcessInstanceAttachmentEntity(
                            null,
                            fileName,
                            context.getThisProcessInstance().getId(),
                            null,
                            null,
                            null,
                            null,
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
            return processDataService
                    .interpolate(context.getProcessData(), configuration.contentHtml);
        }

        if (PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY.equals(contentSource)) {
            var assetKeyStr = processDataService
                    .interpolate(context.getProcessData(), configuration.contentHtmlAssetKey);
            if (StringUtils.isNullOrEmpty(assetKeyStr)) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der Asset-Schluessel fuer die PDF-Vorlage wurde nicht angegeben."
                );
            }

            UUID assetKey;
            try {
                assetKey = UUID.fromString(assetKeyStr.trim());
            } catch (IllegalArgumentException e) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        e,
                        "Der Asset-Schluessel fuer die PDF-Vorlage ist ungueltig: %s",
                        assetKeyStr
                );
            }

            final var asset = retrieveAsset(assetKey);
            return loadAssetContentAsString(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Die HTML-Quellkonfiguration %s ist unbekannt.",
                StringUtils.quote(contentSource)
        );
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
                @ElementPOJOBindingProperty(key = "hint", strValue = "HTML-Template fuer die PDF-Seiten."),
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
