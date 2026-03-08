package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.ProcessDataService;
import de.aivot.GoverBackend.storage.services.StorageService;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.mail.MessagingException;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class EMailActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "mail";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_TO = "to";
    private static final String OUTPUT_NAME_BCC = "bcc";
    private static final String OUTPUT_NAME_SUBJECT = "subject";
    private static final String OUTPUT_NAME_CONTENT = "content";
    private static final String OUTPUT_NAME_ATTACHMENT_FILE_NAMES = "attachmentFileNames";

    private final GoverConfig goverConfig;
    private final ProcessDataService processDataService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final StorageService storageService;
    private final JavaMailSenderImpl mailSender;

    public EMailActionNodeV1(GoverConfig goverConfig,
                             ProcessDataService processDataService,
                             ProcessInstanceAttachmentService processInstanceAttachmentService,
                             StorageService storageService,
                             JavaMailSenderImpl mailSender) {
        this.goverConfig = goverConfig;
        this.processDataService = processDataService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.storageService = storageService;
        this.mailSender = mailSender;
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
        return "E-Mail versenden";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Sendet automatisiert oder manuell E-Mails an ausgewählte Empfänger:innen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(EMailActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für den E-Mail-Versand: %s",
                    e.getMessage()
            );
        }

        layout
                .findChild(EMailActionNodeConfig.EXECUTION_TYPE_FIELD_ID, RadioInputElement.class)
                .ifPresent(select -> {
                    select.setOptions(List.of(
                            RadioInputElementOption.of(EMailActionNodeConfig.EXECUTION_TYPE_AUTOMATIC, "Automatisch versenden"),
                            RadioInputElementOption.of(EMailActionNodeConfig.EXECUTION_TYPE_MANUAL, "Vor dem Versand bearbeiten")
                    ));
                });

        layout
                .findChild(MANUAL_CONTENT_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    var visibilityFunc = ElementVisibilityFunctions
                            .of(NoCodeExpression.of(
                                    NoCodeEqualsOperator.OPERATOR_ID,
                                    new NoCodeReference(EMailActionNodeConfig.EXECUTION_TYPE_FIELD_ID),
                                    new NoCodeStaticValue(EMailActionNodeConfig.EXECUTION_TYPE_MANUAL)
                            ))
                            .recalculateReferencedIds();
                    group.setVisibility(visibilityFunc);
                });

        layout
                .findChild(AUTOMATIC_CONTENT_GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    var visibilityFunc = ElementVisibilityFunctions
                            .of(NoCodeExpression.of(
                                    NoCodeEqualsOperator.OPERATOR_ID,
                                    new NoCodeReference(EMailActionNodeConfig.EXECUTION_TYPE_FIELD_ID),
                                    new NoCodeStaticValue(EMailActionNodeConfig.EXECUTION_TYPE_AUTOMATIC)
                            ))
                            .recalculateReferencedIds();
                    group.setVisibility(visibilityFunc);
                });

        // TODO: Add signature select and attachment select

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "Versendet",
                        "Der Prozess wird hier fortgesetzt, nachdem die E-Mail versendet wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_TO,
                        "Empfänger:innen",
                        "Die Empfänger:innen der versendeten E-Mail."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_BCC,
                        "BCC-Empfänger:innen",
                        "Die BCC-Empfänger:innen der versendeten E-Mail."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_SUBJECT,
                        "Betreff",
                        "Der Betreff der versendeten E-Mail."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONTENT,
                        "Inhalt",
                        "Der HTML-Inhalt der versendeten E-Mail."
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ATTACHMENT_FILE_NAMES,
                        "Anhang-Dateinamen",
                        "Die Dateinamen der als E-Mail-Anhang versendeten Prozess-Anhänge."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        EMailActionNodeConfig configuration;
        try {
            configuration = ElementPOJOMapper
                    .mapToPOJO(context.getThisNode().getConfiguration(), EMailActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des E-Mail-Versand-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        switch (configuration.executionType) {
            case EMailActionNodeConfig.EXECUTION_TYPE_AUTOMATIC -> {
                return initAutomatic(context, configuration);
            }
            case EMailActionNodeConfig.EXECUTION_TYPE_MANUAL -> {
                return initManual(context, configuration);
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Ungültige Ausführungsart für den E-Mail-Versand. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                    StringUtils.quote(EMailActionNodeConfig.EXECUTION_TYPE_AUTOMATIC),
                    StringUtils.quote(EMailActionNodeConfig.EXECUTION_TYPE_MANUAL),
                    StringUtils.quote(configuration.executionType)
            );
        }
    }

    private ProcessNodeExecutionResult initAutomatic(@Nonnull ProcessNodeExecutionContextInit context,
                                                     @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var recipientsStr = processDataService
                .interpolate(context.getProcessData(), config.to);

        if (StringUtils.isNullOrEmpty(recipientsStr)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Empfänger:in für die E-Mail wurde nicht angegeben."
            );
        }
        var recipients = recipientsStr.split(",");

        var recipientsBccStr = processDataService
                .interpolate(context.getProcessData(), config.bcc);
        var recipientsBCC = StringUtils.isNullOrEmpty(recipientsBccStr) ? null : recipientsBccStr.split(",");

        var attachmentFileNamesStr = processDataService
                .interpolate(context.getProcessData(), config.attachmentFileNames);
        var attachmentFileNames = parseAttachmentFileNames(attachmentFileNamesStr);

        var subject = processDataService
                .interpolate(
                        context.getProcessData(),
                        config.automaticContent.subject
                );

        if (StringUtils.isNullOrEmpty(subject)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Betreff für die E-Mail wurde nicht angegeben."
            );
        }

        var contentMarkdown = config.automaticContent.content;

        if (StringUtils.isNullOrEmpty(contentMarkdown)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Inhalt für die E-Mail wurde nicht angegeben."
            );
        }

        Parser parser = Parser.builder().build();
        Node document = parser.parse(contentMarkdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        var contentHtml = renderer.render(document);

        var content =
                processDataService
                        .interpolate(
                                context.getProcessData(),
                                contentHtml
                        );

        if (StringUtils.isNullOrEmpty(content)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Inhalt für die E-Mail wurde nicht angegeben."
            );
        }

        var mimeMessage = mailSender.createMimeMessage();

        try {
            var helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
            helper.setFrom(goverConfig.getFromMail());
            helper.setTo(recipients);
            if (recipientsBCC != null) {
                helper.setBcc(recipientsBCC);
            }
            helper.setSubject(subject);
            helper.setText(content, true);

            for (var attachmentFileName : attachmentFileNames) {
                var attachments = processInstanceAttachmentService
                        .findAllByProcessInstanceIdAndFileName(
                                context.getThisProcessInstance().getId(),
                                attachmentFileName
                        );

                if (attachments.isEmpty()) {
                    throw new ProcessNodeExecutionExceptionMissingValue(
                            "Der Prozess-Anhang mit dem Dateinamen %s wurde in der Prozess-Instanz %d nicht gefunden.",
                            StringUtils.quote(attachmentFileName),
                            context.getThisProcessInstance().getId()
                    );
                }
                if (attachments.size() > 1) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Prozess-Anhang mit dem Dateinamen %s ist in der Prozess-Instanz %d nicht eindeutig vorhanden.",
                            StringUtils.quote(attachmentFileName),
                            context.getThisProcessInstance().getId()
                    );
                }

                var attachment = attachments.get(0);

                try (var attachmentContent = storageService
                        .getDocumentContent(
                                attachment.getStorageProviderId(),
                                attachment.getStoragePathFromRoot()
                        )) {
                    helper.addAttachment(
                            attachmentFileName,
                            new ByteArrayResource(attachmentContent.readAllBytes())
                    );
                } catch (IOException | ResponseException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Der Inhalt des Prozess-Anhangs %s konnte nicht geladen werden: %s",
                            StringUtils.quote(attachmentFileName),
                            e.getMessage()
                    );
                }
            }
        } catch (MessagingException exception) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    exception,
                    "Beim Erstellen der E-Mail ist ein Fehler aufgetreten: %s",
                    exception.getMessage()
            );
        }

        try {
            mailSender
                    .send(mimeMessage);
        } catch (MailException exception) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    exception,
                    "Beim Versenden der E-Mail ist ein Fehler aufgetreten: %s",
                    exception.getMessage()
            );
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_TO, recipients);
        metadata.put(OUTPUT_NAME_BCC, recipientsBCC);
        metadata.put(OUTPUT_NAME_SUBJECT, subject);
        metadata.put(OUTPUT_NAME_CONTENT, content);
        metadata.put(OUTPUT_NAME_ATTACHMENT_FILE_NAMES, attachmentFileNames);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    private ProcessNodeExecutionResult initManual(@Nonnull ProcessNodeExecutionContextInit context,
                                                  @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        throw new ProcessNodeExecutionExceptionUnknown(
                "This functionality is not yet implemented."
        );
    }

    @Nonnull
    private static List<String> parseAttachmentFileNames(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return List.of();
        }

        var result = new ArrayList<String>();
        for (var rawPart : value.split("[,\\n\\r]+")) {
            var part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }
            result.add(part);
        }
        return result;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class EMailActionNodeConfig {
        public static final String RECIPIENT_FIELD_ID = "to";
        public static final String BCC_RECIPIENT_FIELD_ID = "bcc";
        public static final String ATTACHMENT_FILE_NAMES_FIELD_ID = "attachment_file_names";

        public static final String EXECUTION_TYPE_FIELD_ID = "execution_type";
        public static final String EXECUTION_TYPE_MANUAL = "manual";
        public static final String EXECUTION_TYPE_AUTOMATIC = "automatic";

        @InputElementPOJOBinding(id = RECIPIENT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfänger:innen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Kommaseparierte Angabe der Empfänger:innen"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String to;

        @InputElementPOJOBinding(id = BCC_RECIPIENT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "BCC-Empfänger:innen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Angabe weiterer Empfänger:innen als Blind Carbon Copy (BCC)"),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String bcc;

        @InputElementPOJOBinding(id = ATTACHMENT_FILE_NAMES_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateinamen der Anhänge"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Komma- oder zeilengetrennte Dateinamen von Prozessanhängen, die später als E-Mail-Anhänge hinzugefügt werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false),
                @ElementPOJOBindingProperty(key = "isMultiline", boolValue = true)
        })
        public String attachmentFileNames;

        @InputElementPOJOBinding(id = EXECUTION_TYPE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ausführungsart"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Auswahl, ob Nachricht automatisch versendet oder vorher durch eine Sachbearbeiter:in editiert werden soll"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String executionType;

        public EMailActionNodeConfigManualContent manualContent;
        public EMailActionNodeConfigAutomaticContent automaticContent;
    }

    public static final String MANUAL_CONTENT_GROUP_ID = "manual_group";

    @LayoutElementPOJOBinding(id = MANUAL_CONTENT_GROUP_ID, type = ElementType.Group)
    public static class EMailActionNodeConfigManualContent {
        public static final String SUBJECT_FIELD_ID = "manual_subject";
        public static final String CONTENT_FIELD_ID = "manual_content";

        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorlage Betreff der E-Mail"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den Betreff der E-Mail ein."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String subject;

        @InputElementPOJOBinding(id = CONTENT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Vorlage Nachrichtentext"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den Inhalt der E-Mail ein."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "reducedMode", boolValue = true)
        })
        public String content;
    }

    public static final String AUTOMATIC_CONTENT_GROUP_ID = "automatic_group";

    @LayoutElementPOJOBinding(id = AUTOMATIC_CONTENT_GROUP_ID, type = ElementType.Group)
    public static class EMailActionNodeConfigAutomaticContent {
        public static final String SUBJECT_FIELD_ID = "automatic_subject";
        public static final String CONTENT_FIELD_ID = "automatic_content";

        @InputElementPOJOBinding(id = SUBJECT_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Betreff der E-Mail"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den Betreff der E-Mail ein."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String subject;

        @InputElementPOJOBinding(id = CONTENT_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Nachrichtentext"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie den Inhalt der E-Mail ein."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "reducedMode", boolValue = true)
        })
        public String content;
    }
}
