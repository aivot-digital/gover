package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.models.MailSendOptions;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.models.lib.MailAttachmentBytes;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.mail.MessagingException;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class EMailActionNodeV1 implements ProcessNodeDefinition<EMailActionNodeV1.EMailActionNodeConfig> {
    public static final String NODE_KEY = "mail";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_TO = "to";
    private static final String OUTPUT_NAME_BCC = "bcc";
    private static final String OUTPUT_NAME_SUBJECT = "subject";
    private static final String OUTPUT_NAME_CONTENT = "content";
    private static final String OUTPUT_NAME_ATTACHMENT_SET_DATA_KEYS = "attachmentSetDataKeys";

    private final MailService mailService;
    private final TemplateRenderService templateRenderService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final StorageService storageService;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private final ProcessService processService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final ThemeService themeService;
    private final SystemService systemService;

    public EMailActionNodeV1(MailService mailService,
                             TemplateRenderService templateRenderService,
                             ProcessInstanceAttachmentService processInstanceAttachmentService,
                             ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                             StorageService storageService,
                             AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService,
                             ProcessService processService,
                             VDepartmentShadowedService vDepartmentShadowedService,
                             ThemeService themeService,
                             SystemService systemService) {
        this.mailService = mailService;
        this.templateRenderService = templateRenderService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.storageService = storageService;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
        this.processService = processService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.themeService = themeService;
        this.systemService = systemService;
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
        return new ProcessNodeExecutionType[]{
                ProcessNodeExecutionType.Automatic,
                ProcessNodeExecutionType.SemiAutomatic,
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return "E-Mail versenden";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Sendet automatisiert oder manuell E-Mails an ausgewählte Empfänger:innen.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Versendet E-Mails innerhalb eines Prozesses entweder automatisch oder nach manueller Bearbeitung durch eine Mitarbeiter:in.

                Empfänger:innen und Anhänge werden am Prozesselement konfiguriert. Betreff und Inhalt werden entweder automatisch aus Vorlagen erzeugt oder vor dem Versand in einer Aufgabe bearbeitet. Nach dem Versand stellt das Element die verwendeten Nachrichtendaten als Ausgänge bereit.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
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

        layout.findChild(SemiAutomaticMessageConfig.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> SemiAutomaticMessageConfig.initConfigurationLayout(
                        group,
                        context.thisNode().getProcessId(),
                        context.thisNode().getProcessVersion()
                ));
        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "E-Mail versendet",
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
                        "Die Empfänger:innen der versendeten E-Mail.",
                        "Array<string>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_BCC,
                        "BCC-Empfänger:innen",
                        "Die BCC-Empfänger:innen der versendeten E-Mail.",
                        "Array<string> | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_SUBJECT,
                        "Betreff",
                        "Der Betreff der versendeten E-Mail.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONTENT,
                        "Inhalt",
                        "Der HTML-Inhalt der versendeten E-Mail.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ATTACHMENT_SET_DATA_KEYS,
                        "Anlagensätze",
                        "Die Datenschlüssel der als E-Mail-Anhang versendeten Anlagensätze.",
                        "Array<string>"
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<EMailActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        if (SemiAutomaticMessageConfig.isAutomatic(configuration.messageConfig)) {
            return initAutomatic(context, configuration);
        }
        if (SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            return initManual(context, configuration);
        }

        var executionType = configuration.messageConfig == null
                ? null
                : StringUtils.toNullableTrimmedString(configuration.messageConfig.executionType);
        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Ungültige Ausführungsart für den E-Mail-Versand. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC),
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL),
                StringUtils.quote(executionType)
        );
    }

    private ProcessNodeExecutionResult initAutomatic(@Nonnull ProcessNodeExecutionInitContext<EMailActionNodeConfig> context,
                                                     @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var processData = context.getCurrentProcessExecutionData();
        var automaticContent = requireAutomaticContent(config);
        var subject = renderRequiredTemplate(processData, automaticContent.subject, "Betreff");
        var interpolatedContentMarkdown = renderRequiredTemplate(
                processData,
                automaticContent.content,
                "Nachrichtentext"
        );

        return sendMail(subject,
                interpolatedContentMarkdown,
                config,
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance());
    }

    private ProcessNodeExecutionResult initManual(@Nonnull ProcessNodeExecutionInitContext<EMailActionNodeConfig> context,
                                                  @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var manualContent = requireManualContent(config);
        var assigneeUserId = assignmentContextAssigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId(),
                        context.getThisTask().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        manualContent.assignmentContext,
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für das Prozesselement %s konnte keine geeignete Bearbeiter:in im konfigurierten Personenkreis ermittelt werden.",
                        StringUtils.quote(context.getThisNode().getName() != null ? context.getThisNode().getName() : getName())
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setRuntimeData(new LinkedHashMap<>(context.getThisTask().getRuntimeData()))
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<EMailActionNodeConfig> context) throws ResponseException {
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

        var manualContent = requireManualContentForStaffView(context.getConfigurationOfExecutingNode());
        var taskViewData = new AuthoredElementValues();

        try {
            taskViewData.put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    templateRenderService.interpolate(context.getCurrentProcessExecutionData(), manualContent.subject)
            );
            taskViewData.put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    templateRenderService.interpolate(context.getCurrentProcessExecutionData(), manualContent.content)
            );
        } catch (RuntimeException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Die E-Mail-Vorlage konnte nicht gerendert werden: %s",
                    e.getMessage()
            );
        }

        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("Absenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
    }

    private static final String STAFF_TASK_SEND_EVENT = "send";

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<EMailActionNodeConfig> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        if (!STAFF_TASK_SEND_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        var config = context.getConfigurationOfExecutingNode();
        if (!SemiAutomaticMessageConfig.isManual(config.messageConfig)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die E-Mail kann nur im manuellen Ausführungsmodus über eine Aufgabe versendet werden."
            );
        }

        var derivedRuntimeData = new DerivedRuntimeElementData();

        var subject = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_SUBJECT_FIELD_ID));
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(STAFF_TASK_SUBJECT_FIELD_ID, new ComputedElementState()
                    .setError("Der Betreff der E-Mail darf nicht leer sein.")
            );
        }

        var content = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_CONTENT_FIELD_ID));
        if (content == null) {
            derivedRuntimeData.getElementStates().put(STAFF_TASK_CONTENT_FIELD_ID, new ComputedElementState()
                    .setError("Der Inhalt der E-Mail darf nicht leer sein.")
            );
        }

        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }

        var res = sendMail(subject,
                content,
                config,
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance());

        return Optional.of(res);
    }

    @Nonnull
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull EMailActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den automatischen E-Mail-Versand müssen Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull EMailActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.manualContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den manuellen E-Mail-Versand müssen Vorlagen für Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContentForStaffView(
            @Nonnull EMailActionNodeConfig configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    @Nonnull
    private String renderRequiredTemplate(
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull String template,
            @Nonnull String fieldName
    ) throws ProcessNodeExecutionException {
        final String rendered;
        try {
            rendered = StringUtils.toNullableTrimmedString(
                    templateRenderService.interpolate(processExecutionData, template)
            );
        } catch (RuntimeException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Vorlage für %s konnte nicht gerendert werden: %s",
                    fieldName,
                    e.getMessage()
            );
        }

        if (rendered == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der gerenderte Wert für %s ist leer.",
                    fieldName
            );
        }
        return rendered;
    }

    private ProcessNodeExecutionResult sendMail(@Nonnull String subject,
                                                @Nonnull String interpolatedContentMarkdown,
                                                @Nonnull EMailActionNodeConfig config,
                                                @Nonnull ProcessExecutionData processData,
                                                @Nonnull ProcessInstanceEntity processInstance) throws ProcessNodeExecutionException {
        var recipientsStr = StringUtils.toNullableTrimmedString(
                templateRenderService.interpolate(processData, config.to)
        );

        if (recipientsStr == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Empfänger:in für die E-Mail wurde nicht angegeben."
            );
        }
        var recipients = recipientsStr.split(",");

        var recipientsBccStr = StringUtils.toNullableTrimmedString(
                templateRenderService.interpolate(processData, config.bcc)
        );
        var recipientsBCC = recipientsBccStr == null ? null : recipientsBccStr.split(",");

        var attachmentSetDataKeys = config.attachmentSetDataKeys;
        if (attachmentSetDataKeys == null) {
            attachmentSetDataKeys = new ArrayList<>();
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

        if (!mailService.isSendingConfigured()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der E-Mail-Versand ist nicht konfiguriert."
            );
        }

        final var process = retrieveProcess(processInstance);
        final var department = vDepartmentShadowedService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Fachbereich %d des Prozesses %d wurde nicht gefunden.",
                        process.getDepartmentId(),
                        process.getId()
                ));
        final var theme = department.getThemeId() == null
                ? systemService.retrieveDefaultTheme()
                : themeService
                        .retrieve(department.getThemeId())
                        .orElseGet(systemService::retrieveDefaultTheme);
        final var mailAttachments = loadMailAttachments(processInstance, attachmentSetDataKeys);

        var mailContext = new HashMap<String, Object>();
        mailContext.put("title", subject);
        mailContext.put("messageText", interpolatedContentMarkdown);
        mailContext.put("messageHtml", contentHtml);
        mailContext.put("department", department);

        try {
            mailService.sendMail(
                    theme,
                    recipientsStr,
                    Optional.empty(),
                    Optional.ofNullable(recipientsBccStr),
                    subject,
                    MailTemplate.ProcessEmail,
                    mailContext,
                    Optional.empty(),
                    mailAttachments.isEmpty() ? Optional.empty() : Optional.of(mailAttachments),
                    MailSendOptions.defaults()
            );
        } catch (MailException exception) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    exception,
                    "Beim Versenden der E-Mail ist ein Fehler aufgetreten: %s",
                    exception.getMessage()
            );
        } catch (MessagingException | ResponseException | RuntimeException exception) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    exception,
                    "Beim Erstellen der E-Mail ist ein Fehler aufgetreten: %s",
                    exception.getMessage()
            );
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_TO, recipients);
        metadata.put(OUTPUT_NAME_BCC, recipientsBCC);
        metadata.put(OUTPUT_NAME_SUBJECT, subject);
        metadata.put(OUTPUT_NAME_CONTENT, contentHtml);
        metadata.put(OUTPUT_NAME_ATTACHMENT_SET_DATA_KEYS, attachmentSetDataKeys);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    @Nonnull
    private ProcessEntity retrieveProcess(
            @Nonnull ProcessInstanceEntity processInstance
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            return processService
                    .retrieve(processInstance.getProcessId())
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Prozess %d für den E-Mail-Versand wurde nicht gefunden.",
                            processInstance.getProcessId()
                    ));
        } catch (ResponseException exception) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    exception,
                    "Der Prozess %d für den E-Mail-Versand konnte nicht geladen werden: %s",
                    processInstance.getProcessId(),
                    exception.getMessage()
            );
        }
    }

    @Nonnull
    private List<MailAttachmentBytes> loadMailAttachments(
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull List<String> attachmentSetDataKeys
    ) throws ProcessNodeExecutionException {
        var mailAttachments = new ArrayList<MailAttachmentBytes>();
        for (var attachmentSetDataKey : attachmentSetDataKeys) {
            for (var attachment : resolveProcessAttachmentsBySetDataKey(processInstance, attachmentSetDataKey)) {
                try (var attachmentContent = storageService.getDocumentContent(
                        attachment.getStorageProviderId(),
                        attachment.getStoragePathFromRoot()
                )) {
                    var contentType = MediaTypeFactory
                            .getMediaType(attachment.getFileName())
                            .orElse(MediaType.APPLICATION_OCTET_STREAM);
                    mailAttachments.add(new MailAttachmentBytes(
                            attachment.getFileName(),
                            contentType,
                            attachmentContent.readAllBytes()
                    ));
                } catch (IOException | ResponseException exception) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            exception,
                            "Der Inhalt des Prozess-Anhangs %s konnte nicht geladen werden: %s",
                            StringUtils.quote(attachment.getFileName()),
                            exception.getMessage()
                    );
                }
            }
        }
        return mailAttachments;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    private List<ProcessInstanceAttachmentEntity> resolveProcessAttachmentsBySetDataKey(@Nonnull ProcessInstanceEntity processInstance,
                                                                                        @Nonnull String attachmentSetDataKey) throws ProcessNodeExecutionException {
        var normalizedDataKey = StringUtils.toNullableTrimmedString(attachmentSetDataKey);
        if (normalizedDataKey == null) {
            return List.of();
        }

        var attachmentSets = processInstanceAttachmentSetService
                .findAllByProcessInstanceIdAndDataKey(processInstance.getId(), normalizedDataKey);

        if (attachmentSets.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde in der Prozess-Instanz %d nicht gefunden.",
                    StringUtils.quote(normalizedDataKey),
                    processInstance.getId()
            );
        }

        var attachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        for (var attachmentSet : attachmentSets) {
            attachments.addAll(processInstanceAttachmentService.findAllByAttachmentSetId(attachmentSet.getId()));
        }

        if (attachments.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s enthält keine Anhänge.",
                    StringUtils.quote(normalizedDataKey)
            );
        }

        return attachments;
    }

    @Nonnull
    @Override
    public Class<EMailActionNodeConfig> getNodeConfigurationClass() {
        return EMailActionNodeConfig.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class EMailActionNodeConfig {
        public static final String RECIPIENT_FIELD_ID = "to";
        public static final String BCC_RECIPIENT_FIELD_ID = "bcc";
        public static final String ATTACHMENT_SET_DATA_KEYS_FIELD_ID = "attachment_file_names";

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

        @InputElementPOJOBinding(id = ATTACHMENT_SET_DATA_KEYS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensätze"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Anlagensätze der Prozessinstanz, deren Anhänge später als E-Mail-Anhänge hinzugefügt werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public List<String> attachmentSetDataKeys;

        /**
         * Dispatch mode, message templates and optional staff assignment.
         */
        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
