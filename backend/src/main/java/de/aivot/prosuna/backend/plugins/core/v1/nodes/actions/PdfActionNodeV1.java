package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentSetEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinitionMetadata;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.services.PdfService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PdfActionNodeV1 implements ProcessNodeDefinition<PdfActionNodeV1.PdfActionNodeConfig> {
    public static final String NODE_KEY = "pdf";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_FILE_NAME = "fileName";
    private static final String OUTPUT_NAME_MIME_TYPE = "mimeType";
    private static final String OUTPUT_NAME_SIZE_BYTES = "sizeBytes";
    private static final String OUTPUT_NAME_ATTACHMENT_KEY = "attachmentKey";
    private static final String OUTPUT_NAME_STORAGE_PROVIDER_ID = "storageProviderId";
    private static final String OUTPUT_NAME_STORAGE_PATH_FROM_ROOT = "storagePathFromRoot";
    private static final String OUTPUT_NAME_FILES = "files";
    private static final String OUTPUT_FILES_TYPE_DEFINITION =
            "Array<{ name: string; originalFileName: string; uri: string; size: number; }>";
    private static final String HEADER_HTML_SECTION_SEPARATOR = "<!-- KOPFZEILE -->";
    private static final String FOOTER_HTML_SECTION_SEPARATOR = "<!-- FUSSZEILE -->";
    private static final Pattern HTML_DOCUMENT_BLOCK_PATTERN = Pattern.compile(
            "<html\\b.*?</html>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private final PdfService pdfService;
    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final HtmlTemplateInputElementResolver htmlTemplateInputElementResolver;

    public PdfActionNodeV1(PdfService pdfService,
                           TemplateRenderService templateRenderService,
                           ProcessInstanceAttachmentService processInstanceAttachmentService,
                           ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                           HtmlTemplateInputElementResolver htmlTemplateInputElementResolver) {
        this.pdfService = pdfService;
        this.templateRenderService = templateRenderService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.htmlTemplateInputElementResolver = htmlTemplateInputElementResolver;
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
        return CorePlugin.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Dokument erstellen (PDF)";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Generiert ein PDF-Dokument basierend auf einem vordefinierten Template.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
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
                        RadioInputElementOption.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY, "Gespeicherte Vorlage verwenden (Dateien & Medien)"),
                        RadioInputElementOption.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE, "Eigene Dokumentenvorlage als HTML hinterlegen")
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
                .findChild(PdfActionNodeConfig.CONTENT_HTML_ASSET_KEY_FIELD_ID, HtmlTemplateInputElement.class)
                .ifPresent(element -> {
                    element.setVisibility(ElementVisibilityFunctions.of(
                                    NoCodeExpression.of(
                                            NoCodeEqualsOperator.OPERATOR_ID,
                                            NoCodeReference.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_ID),
                                            NoCodeStaticValue.of(PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY)
                                    )
                            )
                            .recalculateReferencedIds());
                });

        return layout;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(PdfActionNodeConfig.CONTENT_HTML_ASSET_KEY_FIELD_ID);
        return configuration;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Dokument erstellt",
                        "Der Prozess wird hier fortgesetzt, nachdem das PDF-Dokument erstellt wurde."
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
                        "Der Dateiname des erzeugten PDF-Dokuments.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_MIME_TYPE,
                        "MIME-Typ",
                        "Der MIME-Typ des erzeugten Dokuments.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ATTACHMENT_KEY,
                        "Anhangs-Schlüssel",
                        "Der Schlüssel des erzeugten Prozess-Anhangs.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_STORAGE_PROVIDER_ID,
                        "Speicheranbieter-ID",
                        "Die ID des Speicheranbieters des erzeugten Prozess-Anhangs.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_STORAGE_PATH_FROM_ROOT,
                        "Speicherpfad",
                        "Der Pfad zum erzeugten Prozess-Anhang im Speicheranbieter.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_SIZE_BYTES,
                        "Dateigröße in Bytes",
                        "Die Größe des erzeugten PDF-Dokuments in Bytes.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_FILES,
                        "Dateien",
                        "Die erzeugten Dateien im Format des Datei-Anlagen-Feldes.",
                        OUTPUT_FILES_TYPE_DEFINITION
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull PdfActionNodeConfig configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        if (StringUtils.isNullOrEmpty(configuration.fileName)) {
            return previousMetadata;
        }

        return ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .addForwardedAttachmentSet(
                        processNodeEntity.getDataKey(),
                        configuration.fileName,
                        null,
                        false,
                        processNodeEntity
                );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<PdfActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        var fileName = templateRenderService
                .interpolate(context.getCurrentProcessExecutionData(), configuration.fileName);
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
                .interpolate(context.getCurrentProcessExecutionData(), pdfHtmlSections.contentHtml);

        if (StringUtils.isNullOrEmpty(interpolatedContentHtml)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der HTML-Inhalt für das PDF konnte nicht interpoliert werden."
            );
        }

        var interpolatedHeaderHtml = templateRenderService
                .interpolate(context.getCurrentProcessExecutionData(), pdfHtmlSections.headerHtml);
        var interpolatedFooterHtml = templateRenderService
                .interpolate(context.getCurrentProcessExecutionData(), pdfHtmlSections.footerHtml);

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
            var attachmentSet = processInstanceAttachmentSetService.create(
                    new ProcessInstanceAttachmentSetEntity()
                            .setName(fileName)
                            .setDataKey(context.getThisNode().getDataKey())
                            .setProcessInstanceId(context.getThisProcessInstance().getId())
                            .setProcessInstanceTaskId(context.getThisTask().getId())
            );

            attachment = processInstanceAttachmentService.create(
                    ProcessInstanceAttachmentEntity.of(
                            fileName,
                            1,
                            context.getThisProcessInstance().getId(),
                            context.getThisTask().getId(),
                            pdfBytes
                    ).setAttachmentSetId(attachmentSet.getId())
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
        try {
            metadata.put(OUTPUT_NAME_FILES, List.of(FileUploadMultipartInputService.buildAttachmentItem(attachment, pdfBytes.length)));
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Erstellen der Dateiliste für das erzeugte PDF: %s",
                    e.getMessage()
            );
        }

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    @Nonnull
    private String resolveContentHtml(@Nonnull ProcessNodeExecutionInitContext<PdfActionNodeConfig> context,
                                      @Nonnull PdfActionNodeConfig configuration) throws ProcessNodeExecutionException {
        var contentSource = configuration.contentHtmlSource;
        if (StringUtils.isNullOrEmpty(contentSource)) {
            contentSource = PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE;
        }

        if (PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_CODE.equals(contentSource)) {
            var res = templateRenderService
                    .interpolate(context.getCurrentProcessExecutionData(), configuration.contentHtml);
            if (StringUtils.isNullOrEmpty(res)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der HTML-Inhalt für das PDF ist leer oder konnte nicht interpoliert werden."
                );
            }
            return res;
        }

        if (PdfActionNodeConfig.CONTENT_HTML_SOURCE_FIELD_OPTION_ASSET_KEY.equals(contentSource)) {
            if (configuration.contentHtmlTemplate == null) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Es muss eine Datei für die HTML-Vorlage ausgewählt sein."
                );
            }

            var resolvedTemplate = htmlTemplateInputElementResolver.resolve(
                    configuration.contentHtmlTemplate,
                    context.getCurrentProcessExecutionData()
            );
            // Render the full asset template before splitting the individual HTML documents so shared
            // blocks defined outside a specific <html> section remain available to all use sites.
            return templateRenderService.interpolate(context.getCurrentProcessExecutionData(), resolvedTemplate);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Die HTML-Quellkonfiguration %s ist unbekannt.",
                StringUtils.quote(contentSource)
        );
    }

    @Nonnull
    private PdfHtmlSections splitHtmlSections(@Nonnull String resolvedHtml) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var headerSeparatorCount = countOccurrences(resolvedHtml, HEADER_HTML_SECTION_SEPARATOR);
        var footerSeparatorCount = countOccurrences(resolvedHtml, FOOTER_HTML_SECTION_SEPARATOR);

        if (headerSeparatorCount > 1) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die PDF-Vorlage enthält mehrere Abschnittstrenner %s.",
                    StringUtils.quote(HEADER_HTML_SECTION_SEPARATOR)
            );
        }
        if (footerSeparatorCount > 1) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die PDF-Vorlage enthält mehrere Abschnittstrenner %s.",
                    StringUtils.quote(FOOTER_HTML_SECTION_SEPARATOR)
            );
        }

        if (headerSeparatorCount == 0 && footerSeparatorCount == 0) {
            return splitSingleContentHtml(resolvedHtml);
        }

        var headerSeparatorIndex = resolvedHtml.indexOf(HEADER_HTML_SECTION_SEPARATOR);
        var footerSeparatorIndex = resolvedHtml.indexOf(FOOTER_HTML_SECTION_SEPARATOR);

        if (headerSeparatorIndex >= 0 && footerSeparatorIndex >= 0 && footerSeparatorIndex < headerSeparatorIndex) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Abschnittstrenner %s muss nach %s stehen.",
                    StringUtils.quote(FOOTER_HTML_SECTION_SEPARATOR),
                    StringUtils.quote(HEADER_HTML_SECTION_SEPARATOR)
            );
        }

        var headerHtml = "";
        var contentAndFooterHtml = resolvedHtml;
        if (headerSeparatorIndex >= 0) {
            headerHtml = normalizeHtmlSection(
                    resolvedHtml.substring(0, headerSeparatorIndex),
                    "Kopfzeile"
            );
            contentAndFooterHtml = resolvedHtml.substring(headerSeparatorIndex + HEADER_HTML_SECTION_SEPARATOR.length());
        }

        var contentHtml = contentAndFooterHtml;
        var footerHtml = "";
        footerSeparatorIndex = contentAndFooterHtml.indexOf(FOOTER_HTML_SECTION_SEPARATOR);
        if (footerSeparatorIndex >= 0) {
            contentHtml = contentAndFooterHtml.substring(0, footerSeparatorIndex);
            footerHtml = normalizeHtmlSection(
                    contentAndFooterHtml.substring(footerSeparatorIndex + FOOTER_HTML_SECTION_SEPARATOR.length()),
                    "Fußzeile"
            );
        }

        contentHtml = normalizeHtmlSection(contentHtml, "Inhalt");
        if (StringUtils.isNullOrEmpty(contentHtml)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die PDF-Vorlage enthält keinen HTML-Abschnitt für den Dokumentinhalt."
            );
        }

        return new PdfHtmlSections(contentHtml, headerHtml, footerHtml);
    }

    @Nonnull
    private PdfHtmlSections splitSingleContentHtml(@Nonnull String resolvedHtml) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var contentHtml = normalizeHtmlSection(resolvedHtml, "Inhalt");
        if (StringUtils.isNullOrEmpty(contentHtml)) {
            return new PdfHtmlSections(resolvedHtml, "", "");
        }

        return new PdfHtmlSections(contentHtml, "", "");
    }

    @Nonnull
    private String normalizeHtmlSection(@Nonnull String htmlSection,
                                        @Nonnull String sectionName) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedSection = htmlSection.trim();
        if (StringUtils.isNullOrEmpty(normalizedSection)) {
            return "";
        }

        var matcher = HTML_DOCUMENT_BLOCK_PATTERN.matcher(normalizedSection);
        if (matcher.find() && matcher.find()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Abschnitt %s der PDF-Vorlage enthält mehrere HTML-Blöcke.",
                    StringUtils.quote(sectionName)
            );
        }

        return normalizedSection;
    }

    private int countOccurrences(@Nonnull String text,
                                 @Nonnull String search) {
        var count = 0;
        var index = 0;
        while ((index = text.indexOf(search, index)) >= 0) {
            count++;
            index += search.length();
        }
        return count;
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

    @Nonnull
    @Override
    public Class<PdfActionNodeConfig> getNodeConfigurationClass() {
        return PdfActionNodeConfig.class;
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
                @ElementPOJOBindingProperty(key = "hint", strValue = "Sie können den Dateinamen ohne Dateiendung oder mit der Dateiendung .pdf angeben."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String fileName;

        @InputElementPOJOBinding(id = CONTENT_HTML_SOURCE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dokumentenvorlage"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie aus, ob eine gespeicherte Dokumentenvorlage aus Dateien & Medien verwendet oder eine eigene Dokumentenvorlage als HTML hinterlegt werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public String contentHtmlSource;

        @InputElementPOJOBinding(id = CONTENT_HTML_CODE_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Eigene Dokumentenvorlage als HTML"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Hinterlegen Sie das vollständige HTML für das PDF-Dokument."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "language", strValue = "html")
        })
        public String contentHtml;

        @InputElementPOJOBinding(id = CONTENT_HTML_ASSET_KEY_FIELD_ID, type = ElementType.HtmlTemplateInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dokumentenvorlage"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie eine gespeicherte HTML-Dokumentenvorlage aus Dateien & Medien aus."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
        })
        public HtmlTemplateInputElementValue contentHtmlTemplate;
    }
}
