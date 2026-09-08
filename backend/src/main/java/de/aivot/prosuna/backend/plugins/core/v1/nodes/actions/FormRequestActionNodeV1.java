package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.FileUploadInputElementItem;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ReplicatingContainerLayoutElementValue;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.filters.ProcessInstanceAttachmentFilter;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.FileUploadMultipartInputService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class FormRequestActionNodeV1 implements ProcessNodeDefinition<FormRequestActionNodeV1.NodeConfig> {
    public static final String NODE_KEY = "form_request";

    private static final String PORT_SUBMITTED = "submitted";

    private static final String OUTPUT_RECIPIENT_IDENTITY_ID = "recipientIdentityId";
    private static final String OUTPUT_PAYLOAD = "payload";
    private static final String OUTPUT_UNMAPPED = "unmapped";
    private static final String OUTPUT_ATTACHMENTS = "attachments";
    private static final String OUTPUT_STARTED = "started";
    private static final String OUTPUT_ATTACHMENTS_TYPE_DEFINITION =
            "Array<{ key: string; fileName: string; originalFileName: string; group: string | null; " +
                    "storageProviderId: number; storagePathFromRoot: string; }>";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_SEND_EVENT = "send";
    private static final String CUSTOMER_TASK_SUBMIT_EVENT = "submit";

    private final TemplateRenderService templateRenderService;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private final ProsunaConfig prosunaConfig;
    private final ElementDataTransformService elementDataTransformService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;

    public FormRequestActionNodeV1(TemplateRenderService templateRenderService,
                                   AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService,
                                   ProsunaConfig prosunaConfig,
                                   ElementDataTransformService elementDataTransformService,
                                   ProcessInstanceAttachmentService processInstanceAttachmentService) {
        this.templateRenderService = templateRenderService;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
        this.prosunaConfig = prosunaConfig;
        this.elementDataTransformService = elementDataTransformService;
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
                ProcessNodeExecutionType.SemiAutomatic
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return "Daten anfordern";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Fordert Daten von einer dritten Person mittels eines Formulars an.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """

                """;
    }

    @Nonnull
    @Override
    public Class<NodeConfig> getNodeConfigurationClass() {
        return NodeConfig.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(NodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        layout.findChild(SemiAutomaticMessageConfig.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> {
                    SemiAutomaticMessageConfig.initConfigurationLayout(
                            group,
                            context.thisNode().getProcessId(),
                            context.thisNode().getProcessVersion()
                    );
                });

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_SUBMITTED,
                        "Daten eingereicht",
                        "Der Prozess wird fortgesetzt, nachdem die Daten erfolgreich eingereicht wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_RECIPIENT_IDENTITY_ID,
                        "Identität",
                        "Die ID der Prozessidentität, an die die Formularanforderung gesendet wurde.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYLOAD,
                        "Zugeordnete Formulardaten",
                        "Enthält alle Formulardaten welche über einen Datenschlüssel zugeordnet wurden.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_UNMAPPED,
                        "Formular-Rohdaten",
                        "Enthält alle Formulardaten unter der jeweiligen Element-ID des Feldes, unabhängig davon, ob ein Element über einen Datenschlüssel zugewiesen wurde oder nicht.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_ATTACHMENTS,
                        "Anlagen",
                        "Eine Liste aller Anlagen, die über dieses Formular hochgeladen wurden.",
                        OUTPUT_ATTACHMENTS_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        OUTPUT_STARTED,
                        "Eingangszeitstempel",
                        "Der Zeitstempel des Dateneingangs an den Auslöser",
                        "string"
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<NodeConfig> context) throws ProcessNodeExecutionException {
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
                "Ungültige Ausführungsart für die Formularanforderung. Erwartet werden entweder %s oder %s. Übergeben wurde: %s",
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_AUTOMATIC),
                StringUtils.quote(SemiAutomaticMessageConfig.LayoutConfig.EXECUTION_TYPE_MANUAL),
                StringUtils.quote(executionType)
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initAutomatic(
            @Nonnull ProcessNodeExecutionInitContext<NodeConfig> context,
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var automaticContent = requireAutomaticContent(configuration);

        var subject = renderRequiredTemplate(
                context.getCurrentProcessExecutionData(),
                automaticContent.subject,
                "Betreff"
        );

        var content = renderRequiredTemplate(
                context.getCurrentProcessExecutionData(),
                automaticContent.content,
                "Nachrichtentext"
        );

        return createCustomerAssignmentResult(
                context.getThisProcessInstance(),
                context.getThisTask(),
                configuration,
                subject,
                content
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initManual(
            @Nonnull ProcessNodeExecutionInitContext<NodeConfig> context,
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var manualContent = requireManualContent(configuration);

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
                        StringUtils.quote(context.getThisNode().resolveName(this))
                ));

        var runtimeData = new LinkedHashMap<>(context.getThisTask().getRuntimeData());

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setRuntimeData(runtimeData)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context
    ) throws ResponseException {
        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der Aufforderung");
        subjectField.setRequired(true);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Nachricht der Aufforderung");
        contentField.setRequired(true);

        var root = new GroupLayoutElement();
        root.setId(STAFF_TASK_ROOT_ID);
        root.setChildren(new LinkedList<>(List.of(subjectField, contentField)));

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
                    "Die Nachrichtenvorlage der Aufforderung konnte nicht gerendert werden: %s",
                    e.getMessage()
            );
        }

        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("Aufforderung versenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context,
            @Nonnull AuthoredElementValues update,
            @Nonnull String event
    ) throws ResponseException, ProcessNodeExecutionException {
        if (!STAFF_TASK_SEND_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        var configuration = context.getConfigurationOfExecutingNode();
        if (!SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Aufforderung kann nur im manuellen Ausführungsmodus über eine Aufgabe versendet werden."
            );
        }

        var subject = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_SUBJECT_FIELD_ID));
        var content = StringUtils.toNullableTrimmedString(update.get(STAFF_TASK_CONTENT_FIELD_ID));
        validateStaffMessage(subject, content);

        var result = createCustomerAssignmentResult(
                context.getThisProcessInstance(),
                context.getThisTask(),
                configuration,
                subject,
                content
        );

        return Optional.of(result);
    }

    private ProcessNodeExecutionResult createCustomerAssignmentResult(ProcessInstanceEntity processInstance,
                                                                      ProcessInstanceTaskEntity task,
                                                                      NodeConfig configuration,
                                                                      String subject,
                                                                      String content) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var recipientId = requireRecipientIdentity(configuration.recipientIdentityId);

        var customerLink = prosunaConfig
                .createUrl("/process/", processInstance.getAccessKey(), "tasks", task.getAccessKey());

        var message = CommunicationMessage.of(
                subject,
                content,
                content,
                List.of(new CommunicationMessageCallToAction("Daten einreichen", customerLink)),
                List.of()
        );

        var communicationRequest = new ProcessNodeExecutionResultCommunicationRequest(
                recipientId,
                message
        );

        return new ProcessNodeExecutionResultTaskAssignedCustomer()
                .setIdentityId(recipientId)
                .setCommunicationRequest(communicationRequest);
    }

    @Nonnull
    @Override
    public ProcessNodeCustomerView getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context) throws ResponseException {
        return ProcessNodeCustomerView.of(
                context,
                context.getConfigurationOfExecutingNode().uiDefinition,
                List.of(new TaskViewEvent("Daten einreichen", CUSTOMER_TASK_SUBMIT_EVENT)),
                new AuthoredElementValues(),
                context.getConfigurationOfExecutingNode().recipientIdentityId
        );
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context,
                                                                            @Nonnull AuthoredElementValues update,
                                                                            @Nonnull DerivedRuntimeElementData derived,
                                                                            @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        if (!CUSTOMER_TASK_SUBMIT_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Das Event %s wird von diesem Prozesselement nicht unterstützt.",
                    StringUtils.quote(event)
            );
        }

        var effectiveValues = derived.getEffectiveValues();
        var configuration = context.getConfigurationOfExecutingNode();
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(
                OUTPUT_RECIPIENT_IDENTITY_ID,
                requireRecipientIdentity(configuration.recipientIdentityId)
        );
        nodeData.put(
                OUTPUT_PAYLOAD,
                elementDataTransformService.buildPayload(
                        configuration.uiDefinition,
                        effectiveValues,
                        derived.getElementStates()
                )
        );
        nodeData.put(OUTPUT_UNMAPPED, effectiveValues);
        nodeData.put(OUTPUT_ATTACHMENTS, resolveSubmittedAttachments(context, effectiveValues));
        nodeData.put(OUTPUT_STARTED, Instant.now());

        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_SUBMITTED)
                .setNodeData(nodeData);

        return Optional.of(result);
    }

    @Nonnull
    private List<Map<String, Object>> resolveSubmittedAttachments(
            @Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context,
            @Nonnull Map<String, Object> effectiveValues
    ) throws ResponseException {
        var referencedAttachmentKeys = new LinkedHashSet<UUID>();
        collectReferencedAttachmentKeys(effectiveValues, referencedAttachmentKeys);
        if (referencedAttachmentKeys.isEmpty()) {
            return List.of();
        }

        var processInstanceId = context.getThisProcessInstance().getId();
        var processInstanceTaskId = context.getThisTask().getId();
        var attachmentsByKey = new HashMap<UUID, ProcessInstanceAttachmentEntity>();
        processInstanceAttachmentService
                .list(ProcessInstanceAttachmentFilter
                        .create()
                        .setProcessInstanceTaskId(processInstanceTaskId))
                .forEach(attachment -> {
                    if (Objects.equals(attachment.getProcessInstanceId(), processInstanceId)
                            && Objects.equals(attachment.getProcessInstanceTaskId(), processInstanceTaskId)) {
                        attachmentsByKey.put(attachment.getKey(), attachment);
                    }
                });

        return referencedAttachmentKeys
                .stream()
                .map(attachmentsByKey::get)
                .filter(Objects::nonNull)
                .map(FormRequestActionNodeV1::createAttachmentOutput)
                .toList();
    }

    private static void collectReferencedAttachmentKeys(
            @Nullable Object value,
            @Nonnull Set<UUID> attachmentKeys
    ) {
        if (value instanceof FileUploadInputElementItem fileItem) {
            addReferencedAttachmentKey(fileItem.getUri(), attachmentKeys);
            return;
        }

        if (value instanceof ReplicatingContainerLayoutElementValue replicatedValue) {
            collectReferencedAttachmentKeys(replicatedValue.getValues(), attachmentKeys);
            return;
        }

        if (value instanceof Map<?, ?> map) {
            var uri = map.get("uri");
            if (uri instanceof String stringUri) {
                addReferencedAttachmentKey(stringUri, attachmentKeys);
            }
            map.values().forEach(child -> collectReferencedAttachmentKeys(child, attachmentKeys));
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(child -> collectReferencedAttachmentKeys(child, attachmentKeys));
        }
    }

    private static void addReferencedAttachmentKey(
            @Nullable String uri,
            @Nonnull Set<UUID> attachmentKeys
    ) {
        if (uri == null || !uri.startsWith(FileUploadMultipartInputService.PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX)) {
            return;
        }

        var rawAttachmentKey = uri
                .substring(FileUploadMultipartInputService.PROCESS_INSTANCE_ATTACHMENT_URI_PREFIX.length())
                .trim();
        try {
            attachmentKeys.add(UUID.fromString(rawAttachmentKey));
        } catch (IllegalArgumentException ignored) {
            // Invalid or stale attachment references are not exposed as task outputs.
        }
    }

    @Nonnull
    private static Map<String, Object> createAttachmentOutput(@Nonnull ProcessInstanceAttachmentEntity attachment) {
        var attachmentOutput = new LinkedHashMap<String, Object>();
        attachmentOutput.put("key", attachment.getKey());
        attachmentOutput.put("fileName", attachment.getFileName());
        attachmentOutput.put("originalFileName", attachment.getOriginalFileName());
        attachmentOutput.put("group", attachment.getGroup());
        attachmentOutput.put("storageProviderId", attachment.getStorageProviderId());
        attachmentOutput.put("storagePathFromRoot", attachment.getStoragePathFromRoot());
        return attachmentOutput;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    private String requireRecipientIdentity(@Nullable String recipientIdentity) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedIdentity = StringUtils.toNullableTrimmedString(recipientIdentity);
        if (normalizedIdentity == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die Formularanforderung muss eine Empfängeridentität konfiguriert sein.");
        }
        return normalizedIdentity;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null || StringUtils.isNullOrEmpty(content.subject) || StringUtils.isNullOrEmpty(content.content)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den automatischen Versand müssen Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull NodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.manualContent;
        if (content == null || StringUtils.isNullOrEmpty(content.subject) || StringUtils.isNullOrEmpty(content.content)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den manuellen Versand müssen Vorlagen für Betreff und Nachrichtentext konfiguriert sein."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContentForStaffView(
            @Nonnull NodeConfig configuration
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

    private static void validateStaffMessage(
            @Nullable String subject,
            @Nullable String content
    ) throws ResponseException {
        var derivedRuntimeData = new DerivedRuntimeElementData();
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    new ComputedElementState().setError("Der Betreff der Zahlungsaufforderung darf nicht leer sein.")
            );
        }
        if (content == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    new ComputedElementState().setError("Die Nachricht der Zahlungsaufforderung darf nicht leer sein.")
            );
        }
        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }
    }

    /**
     * Configuration for creating a payable customer task and sending its payment request to one process identity.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class NodeConfig {
        public static final String RECIPIENT_IDENTITY_ID_FIELD_ID = "recipientIdentityId";
        public static final String UI_DEFINITION_FIELD_ID = "uiDefinition";

        /**
         * Logical process identity receiving the form request. A missing identity or an identity that is not
         * present in the process instance prevents initialization or dispatch.
         */
        @InputElementPOJOBinding(id = RECIPIENT_IDENTITY_ID_FIELD_ID, type = ElementType.ProcessIdentityIdInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfängeridentität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Identität, an die die Anforderung über den ausgewählten Kommunikationsweg gesendet wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String recipientIdentityId;

        /**
         * UI-Definition for the form request. This is a required field and must be a valid UI definition element.
         */
        @InputElementPOJOBinding(id = UI_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Formular"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Konfiguration des Formulars."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public GroupLayoutElement uiDefinition;

        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
