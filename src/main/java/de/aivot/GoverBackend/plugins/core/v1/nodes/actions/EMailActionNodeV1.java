package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementState;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
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
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.*;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.GoverBackend.process.services.ProcessInstanceAttachmentService;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
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

import java.awt.*;
import java.io.IOException;
import java.util.*;
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
    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final StorageService storageService;
    private final JavaMailSenderImpl mailSender;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;

    public EMailActionNodeV1(GoverConfig goverConfig,
                             TemplateRenderService templateRenderService,
                             ProcessInstanceAttachmentService processInstanceAttachmentService,
                             StorageService storageService,
                             JavaMailSenderImpl mailSender, AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService) {
        this.goverConfig = goverConfig;
        this.templateRenderService = templateRenderService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.storageService = storageService;
        this.mailSender = mailSender;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
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

        layout
                .findChild(EMailActionNodeConfigManualContent.ASSIGNMENT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(assignment -> {
                    assignment.setAllowedTypes(List.of(
                            AssignmentContextInputElement.ALLOWED_TYPE_ORG_UNIT,
                            AssignmentContextInputElement.ALLOWED_TYPE_TEAM,
                            AssignmentContextInputElement.ALLOWED_TYPE_USER
                    ));

                    var accessConstraints = new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(context.processDefinition().getId())
                            .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                            .setRequiredPermissions(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK));
                    assignment.setProcessAccessConstraint(accessConstraints);
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
                    .mapToPOJO(context.getConfiguration().getEffectiveValues(), EMailActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des E-Mail-Versand-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        ProcessNodeExecutionResult result;
        switch (configuration.executionType) {
            case EMailActionNodeConfig.EXECUTION_TYPE_AUTOMATIC -> {
                result = initAutomatic(context, configuration);
            }
            case EMailActionNodeConfig.EXECUTION_TYPE_MANUAL -> {
                result = initManual(context, configuration);
            }
            default -> throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Ungültige Ausführungsart für den E-Mail-Versand. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                    StringUtils.quote(EMailActionNodeConfig.EXECUTION_TYPE_AUTOMATIC),
                    StringUtils.quote(EMailActionNodeConfig.EXECUTION_TYPE_MANUAL),
                    StringUtils.quote(configuration.executionType)
            );
        }

        result.setProcessData(context.getProcessData());

        return result;
    }

    private ProcessNodeExecutionResult initAutomatic(@Nonnull ProcessNodeExecutionContextInit context,
                                                     @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var processData = context.getProcessData();

        var subject = templateRenderService
                .interpolate(
                        processData,
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

        var interpolatedContentMarkdown =
                templateRenderService
                        .interpolate(
                                processData,
                                contentMarkdown
                        );

        if (StringUtils.isNullOrEmpty(interpolatedContentMarkdown)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Inhalt für die E-Mail ist nach der Verarbeitung leer. Bitte überprüfen Sie die Vorlage und die Prozessdaten.",
                    StringUtils.quote(contentMarkdown)
            );
        }

        return sendMail(subject, interpolatedContentMarkdown, config, context.getProcessData(), context.getThisProcessInstance());
    }

    private ProcessNodeExecutionResult initManual(@Nonnull ProcessNodeExecutionContextInit context,
                                                  @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var assigneeUserId = assignmentContextAssigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        config.manualContent.assignmentContext,
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für das Prozesselement %s konnte keine geeignete Bearbeiter:in im konfigurierten Personenkreis ermittelt werden.",
                        StringUtils.quote(context.getThisNode().getName() != null ? context.getThisNode().getName() : getName())
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId);
    }

    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";

    @Nonnull
    @Override
    public LayoutElement<?> getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        var root = new GroupLayoutElement();
        root.setId("root");
        root.setChildren(new LinkedList<>());

        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der E-Mail");
        subjectField.setRequired(true);
        root.getChildren().add(subjectField);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Inhalt der E-Mail");
        contentField.setRequired(true);
        root.getChildren().add(contentField);

        return root;
    }

    @Override
    public AuthoredElementValues getStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        EMailActionNodeConfig config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(context.getRuntimeElementData().getEffectiveValues(), EMailActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Die Konfiguration des E-Mail-Versand-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        var authoredValues = new AuthoredElementValues();

        var subject = templateRenderService
                .interpolate(
                        context.getProcessData(),
                        config.manualContent.subject
                );
        authoredValues.put(STAFF_TASK_SUBJECT_FIELD_ID, subject);

        var content = templateRenderService
                .interpolate(
                        context.getProcessData(),
                        config.manualContent.content
                );
        authoredValues.put(STAFF_TASK_CONTENT_FIELD_ID, content);

        return authoredValues;
    }

    private static final String STAFF_TASK_SEND_EVENT = "send";

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return List.of(
                new TaskViewEvent(
                        "Absenden",
                        STAFF_TASK_SEND_EVENT
                )
        );
    }

    @Override
    public Optional<ProcessNodeExecutionResult> onUpdateFromStaff(@Nonnull ProcessNodeExecutionContextUIStaff context,
                                                                  @Nonnull AuthoredElementValues update,
                                                                  @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        if (!event.equals(STAFF_TASK_SEND_EVENT)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        EMailActionNodeConfig config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(context.getRuntimeElementData().getEffectiveValues(), EMailActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des E-Mail-Versand-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        var derivedRuntimeData = new DerivedRuntimeElementData();

        var subject = (String) update.getOrDefault(STAFF_TASK_SUBJECT_FIELD_ID, null);
        if (StringUtils.isNullOrEmpty(subject)) {
            derivedRuntimeData.getElementStates().put(STAFF_TASK_SUBJECT_FIELD_ID, new ComputedElementState()
                    .setError("Der Betreff der E-Mail darf nicht leer sein.")
            );
        }

        var content =  (String) update.getOrDefault(STAFF_TASK_CONTENT_FIELD_ID, null);
        if (StringUtils.isNullOrEmpty(content)) {
            derivedRuntimeData.getElementStates().put(STAFF_TASK_CONTENT_FIELD_ID, new ComputedElementState()
                    .setError("Der Inhalt der E-Mail darf nicht leer sein.")
            );
        }

        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }

        var res = sendMail(subject, content, config, context.getProcessData(), context.getThisProcessInstance());

        return Optional.of(res);
    }

    private ProcessNodeExecutionResult sendMail(@Nonnull String subject,
                                                @Nonnull String interpolatedContentMarkdown,
                                                @Nonnull EMailActionNodeConfig config,
                                                @Nonnull ProcessExecutionData processData,
                                                @Nonnull ProcessInstanceEntity processInstance) throws ProcessNodeExecutionException {
        var recipientsStr = templateRenderService
                .interpolate(processData, config.to);

        if (StringUtils.isNullOrEmpty(recipientsStr)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Empfänger:in für die E-Mail wurde nicht angegeben."
            );
        }
        var recipients = recipientsStr.split(",");

        var recipientsBccStr = templateRenderService
                .interpolate(processData, config.bcc);
        var recipientsBCC = StringUtils.isNullOrEmpty(recipientsBccStr) ? null : recipientsBccStr.split(",");

        var attachmentFileNames = config.attachmentFileNames;
        if (attachmentFileNames == null) {
            attachmentFileNames = new ArrayList<>();
        }

        Parser parser = Parser.builder().build();
        Node document = parser.parse(interpolatedContentMarkdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        var contentHtml = renderer.render(document);

        if (StringUtils.isNullOrEmpty(contentHtml)) {
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
            helper.setText(contentHtml, true);

            for (var attachmentFileName : attachmentFileNames) {
                var attachments = processInstanceAttachmentService
                        .findAllByProcessInstanceIdAndFileName(
                                processInstance.getId(),
                                attachmentFileName
                        );

                if (attachments.isEmpty()) {
                    throw new ProcessNodeExecutionExceptionMissingValue(
                            "Der Prozess-Anhang mit dem Dateinamen %s wurde in der Prozess-Instanz %d nicht gefunden.",
                            StringUtils.quote(attachmentFileName),
                            processInstance.getId()
                    );
                }
                if (attachments.size() > 1) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Prozess-Anhang mit dem Dateinamen %s ist in der Prozess-Instanz %d nicht eindeutig vorhanden.",
                            StringUtils.quote(attachmentFileName),
                            processInstance.getId()
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
        } catch (MessagingException | ProcessNodeExecutionExceptionUnknown exception) {
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
        metadata.put(OUTPUT_NAME_CONTENT, contentHtml);
        metadata.put(OUTPUT_NAME_ATTACHMENT_FILE_NAMES, attachmentFileNames);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(EMailActionNodeConfigManualContent.ASSIGNMENT_FIELD_ID);
        return configuration;
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

        @InputElementPOJOBinding(id = ATTACHMENT_FILE_NAMES_FIELD_ID, type = ElementType.ChipInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Dateinamen der Anhänge"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Dateinamen von Prozessanhängen, die später als E-Mail-Anhänge hinzugefügt werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public List<String> attachmentFileNames;

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

    @LayoutElementPOJOBinding(id = MANUAL_CONTENT_GROUP_ID, type = ElementType.GroupLayout)
    public static class EMailActionNodeConfigManualContent {
        public static final String SUBJECT_FIELD_ID = "manual_subject";
        public static final String CONTENT_FIELD_ID = "manual_content";
        public static final String ASSIGNMENT_FIELD_ID = "manual_assignment";

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

        @InputElementPOJOBinding(id = ASSIGNMENT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der für diese Aufgabe herangezogen werden kann."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Organisationseinheit, Team oder Mitarbeiter:in suchen"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }

    public static final String AUTOMATIC_CONTENT_GROUP_ID = "automatic_group";

    @LayoutElementPOJOBinding(id = AUTOMATIC_CONTENT_GROUP_ID, type = ElementType.GroupLayout)
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
