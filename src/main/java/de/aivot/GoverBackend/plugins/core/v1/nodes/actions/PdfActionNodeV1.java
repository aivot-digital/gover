package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.ProcessNodeOutput;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

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

    public PdfActionNodeV1(PdfService pdfService,
                           ProcessDataService processDataService,
                           ProcessInstanceAttachmentService processInstanceAttachmentService) {
        this.pdfService = pdfService;
        this.processDataService = processDataService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
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
        try {
            return ElementPOJOMapper
                    .createFromPOJO(PdfActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für die PDF-Erstellung: %s",
                    e.getMessage()
            );
        }
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

        var contentHtml = processDataService
                .interpolate(context.getProcessData(), configuration.contentHtml);
        if (StringUtils.isNullOrEmpty(contentHtml)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der HTML-Inhalt fuer das PDF wurde nicht angegeben."
            );
        }

        var headerHtml = processDataService
                .interpolate(context.getProcessData(), configuration.headerHtml);
        var footerHtml = processDataService
                .interpolate(context.getProcessData(), configuration.footerHtml);
        var paperWidth = processDataService
                .interpolate(context.getProcessData(), configuration.paperWidth);
        var paperHeight = processDataService
                .interpolate(context.getProcessData(), configuration.paperHeight);

        byte[] pdfBytes;
        try {
            pdfBytes = pdfService.generatePdfFromHtml(
                    contentHtml,
                    headerHtml,
                    footerHtml,
                    paperWidth,
                    paperHeight
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

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class PdfActionNodeConfig {
        public static final String FILE_NAME_FIELD_ID = "file_name";
        public static final String CONTENT_HTML_FIELD_ID = "content_html";
        public static final String HEADER_HTML_FIELD_ID = "header_html";
        public static final String FOOTER_HTML_FIELD_ID = "footer_html";
        public static final String PAPER_WIDTH_FIELD_ID = "paper_width";
        public static final String PAPER_HEIGHT_FIELD_ID = "paper_height";

        @InputElementPOJOBinding(id = FILE_NAME_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateiname"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Dateiname des PDF-Dokuments (z.B. dokument.pdf)."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String fileName;

        @InputElementPOJOBinding(id = CONTENT_HTML_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "HTML-Inhalt"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "HTML-Template fuer die PDF-Seiten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String contentHtml;

        @InputElementPOJOBinding(id = HEADER_HTML_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Header HTML (optional)"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionales HTML fuer den Seitenkopf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String headerHtml;

        @InputElementPOJOBinding(id = FOOTER_HTML_FIELD_ID, type = ElementType.CodeInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Footer HTML (optional)"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionales HTML fuer die Seitenfusszeile."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String footerHtml;

        @InputElementPOJOBinding(id = PAPER_WIDTH_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Papierbreite"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optional, Standard ist 210mm."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String paperWidth;

        @InputElementPOJOBinding(id = PAPER_HEIGHT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Papierhoehe"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optional, Standard ist 297mm."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String paperHeight;
    }
}
