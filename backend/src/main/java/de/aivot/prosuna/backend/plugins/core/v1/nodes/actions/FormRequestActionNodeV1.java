package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.zxing.WriterException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.LayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FormRequestActionNodeV1 implements ProcessNodeDefinition<FormRequestActionNodeV1.NodeConfig> {
    public static final String NODE_KEY = "form_request";

    private static final String PORT_SUBMITTED = "submitted";

    private static final String OUTPUT_RECIPIENT_IDENTITY_ID = "recipientIdentityId";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_SEND_EVENT = "send";
    private static final String CUSTOMER_TASK_SUBMIT_EVENT = "submit";

    private final TemplateRenderService templateRenderService;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;

    public FormRequestActionNodeV1(TemplateRenderService templateRenderService,
                                   AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService) {
        this.templateRenderService = templateRenderService;
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
                        "Die ID der Prozessidentität, an die die Zahlungsaufforderung gesendet wurde.",
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

        return createCustomerAssignmentResult(configuration, subject, content);
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
    public LayoutElement<?> getStaffTaskView(
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
        return root;
    }

    @Nonnull
    @Override
    public AuthoredElementValues createDefaultStaffTaskViewData(
            @Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context
    ) throws ResponseException {
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

        return taskViewData;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(
            @Nonnull ProcessNodeExecutionContextUIStaff<NodeConfig> context
    ) {
        return List.of(new TaskViewEvent(
                "Aufforderung versenden",
                STAFF_TASK_SEND_EVENT
        ));
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

        var result = createCustomerAssignmentResult(configuration, subject, content);

        return Optional.of(result);
    }

    private ProcessNodeExecutionResult createCustomerAssignmentResult(NodeConfig configuration, String subject, String content) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var recipientId = requireRecipientIdentity(configuration.recipientIdentityId);
        var message = CommunicationMessage.of(subject, content, content);

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
    public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context) throws ResponseException {
        return context.getConfigurationOfExecutingNode().uiDefinition;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getCustomerTaskViewEvents(@Nonnull ProcessNodeExecutionContextUICustomer<NodeConfig> context) throws ResponseException {
        return List.of(new TaskViewEvent(
                "Daten einreichen",
                CUSTOMER_TASK_SUBMIT_EVENT
        ));
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

        var nodeData = new HashMap<String, Object>();

        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_SUBMITTED)
                .setNodeData(nodeData);

        return Optional.of(result);
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
