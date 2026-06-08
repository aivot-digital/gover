package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ElementDisplayContext;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.SpacerContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.*;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.*;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.GoverBackend.process.services.TemplateRenderService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.utils.IsoTimestampUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ApprovalActionNodeV1 implements ProcessNodeDefinition<ApprovalActionNodeV1.ApprovalConfiguration> {
    public static final String NODE_KEY = "approval";

    private static final String CRITERIA_FIELD_ID = "criteria";
    private static final String CONTENT_MODE_FIELD_ID = "contentMode";
    private static final String DATA_CONTENT_FIELD_ID = "dataContent";
    private static final String CUSTOM_CONTENT_FIELD_ID = "customContent";
    private static final String ASSIGNMENT_CONTEXT_FIELD_ID = "assignmentContext";

    private static final String TASK_VIEW_ROOT_ID = "approval-task-view";
    private static final String TASK_VIEW_REMARK_FIELD_ID = "approvalRemark";
    private static final String TASK_VIEW_ACTIONS_SPACER_ID = "approval-actions-spacer";

    private static final String MODE_DATA = "data";
    private static final String MODE_CUSTOM_CONTENT = "custom";

    private static final String PORT_APPROVED = "approved";
    private static final String PORT_REJECTED = "rejected";

    private static final String EVENT_APPROVE = "approve";
    private static final String EVENT_REJECT = "reject";

    private static final String OUTPUT_DECISION = "decision";
    private static final String OUTPUT_REMARK = "remark";
    private static final String OUTPUT_PROCESSED_BY_USER_ID = "processedByUserId";
    private static final String OUTPUT_PROCESSED_AT = "processedAt";

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ElementDataTransformService elementDataTransformService;
    private final TemplateRenderService templateRenderService;

    public ApprovalActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService, ElementDataTransformService elementDataTransformService, TemplateRenderService templateRenderService) {
        this.assigneeResolverService = assigneeResolverService;
        this.elementDataTransformService = elementDataTransformService;
        this.templateRenderService = templateRenderService;
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
    public String getName() {
        return "Freigabe einholen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Holt eine Freigabe durch eine Mitarbeiter:in aus einem definierten Personenkreis ein.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(ApprovalConfiguration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }

        // Configure the content mode field
        layout
                .findChild(CONTENT_MODE_FIELD_ID, RadioInputElement.class)
                .ifPresent(contentModeField -> {
                    contentModeField.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(MODE_DATA)));
                    contentModeField.setOptions(List.of(
                            RadioInputElementOption.of(MODE_DATA, "Daten"),
                            RadioInputElementOption.of(MODE_CUSTOM_CONTENT, "Eigene Inhalte")
                    ));
                });

        // Configure the ui definition modeling field
        layout
                .findChild(DATA_CONTENT_FIELD_ID, UiDefinitionInputElement.class)
                .ifPresent(dataContentField -> {
                    dataContentField.setElementType(ElementType.GroupLayout);
                    dataContentField.setVisibility(buildModeVisibility(MODE_DATA));
                    dataContentField.setDisplayContext(ElementDisplayContext.StaffFacing);
                });

        // Configure the text content field
        layout
                .findChild(CUSTOM_CONTENT_FIELD_ID, RichTextInputElement.class)
                .ifPresent(customContentField -> {
                    customContentField.setVisibility(buildModeVisibility(MODE_CUSTOM_CONTENT));
                });

        // Configure the assignment field
        layout
                .findChild(ASSIGNMENT_CONTEXT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(assignmentContextField -> {
                    assignmentContextField.setAllowedTypes(List.of("orgUnit", "team", "user"));
                    assignmentContextField.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(context.processDefinition().getId())
                            .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                            .setRequiredPermissions(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)));
                });

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_APPROVED,
                        "Freigegeben",
                        "Der Prozess wird fortgesetzt, wenn die Freigabe erteilt wurde."
                ),
                new ProcessNodePort(
                        PORT_REJECTED,
                        "Abgelehnt",
                        "Der Prozess wird fortgesetzt, wenn die Freigabe abgelehnt wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_DECISION,
                        "Entscheidung",
                        "Die getroffene Entscheidung, entweder 'approved' oder 'rejected'."
                ),
                new ProcessNodeOutput(
                        OUTPUT_REMARK,
                        "Vermerk",
                        "Der bei der Freigabe oder Ablehnung erfasste Vermerk."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_BY_USER_ID,
                        "Bearbeitet durch",
                        "Die ID der Mitarbeiter:in, die die Entscheidung getroffen hat."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_AT,
                        "Bearbeitet am",
                        "Der Zeitstempel der Entscheidung im ISO-Format."
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<ApprovalConfiguration> context) throws ProcessNodeExecutionException {
        var config = context.getConfigurationOfExecutingNode();
        var workingProcessData = extractWorkingProcessData(context.getCurrentProcessExecutionData());

        var assigneeUserId = assigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        config.assignmentContext,
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für das Prozesselement %s konnte keine geeignete Bearbeiter:in im konfigurierten Personenkreis ermittelt werden.",
                        StringUtils.quote(this.resolveNodeName(context.getThisNode()))
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setProcessData(workingProcessData)
                .setRuntimeData(Map.of());
    }

    @Nonnull
    @Override
    public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context) throws ResponseException {
        var config = context.getConfigurationOfExecutingNode();

        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var children = new ArrayList<BaseFormElement>();
        var criteriaHeadline = new HeadlineContentElement();
        criteriaHeadline.setId("approval-criteria-headline");
        criteriaHeadline.setContent("Freigabekriterien");
        children.add(criteriaHeadline);

        var criteriaContent = new RichTextContentElement();
        criteriaContent.setId("approval-criteria-content");
        var renderedCriteria = templateRenderService
                .interpolate(context.getCurrentProcessExecutionData(), config.criteria);
        criteriaContent.setContent(renderedCriteria);
        children.add(criteriaContent);

        var contentHeadline = new HeadlineContentElement();
        contentHeadline.setId("approval-content-headline");
        contentHeadline.setContent("Inhalte");
        children.add(contentHeadline);

        if (MODE_DATA.equals(config.contentMode)) {
            children.add(config.dataContent);
        } else {
            var customContent = new RichTextContentElement();
            customContent.setId("approval-custom-content");
            customContent.setContent(config.customContent);
            children.add(customContent);
        }

        var remarkField = new RichTextInputElement();
        remarkField.setId(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setDestinationKey(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setLabel("Vermerk");
        remarkField.setHint("Optionaler Vermerk zur Freigabeentscheidung.");
        remarkField.setRequired(false);
        remarkField.setWeight(6.0);
        children.add(remarkField);

        var actionsSpacer = new SpacerContentElement();
        actionsSpacer.setId(TASK_VIEW_ACTIONS_SPACER_ID);
        actionsSpacer.setHeight("8");
        children.add(actionsSpacer);

        layout.setChildren(children);
        return layout;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context) {
        return List.of(
                new TaskViewEvent("Freigeben", EVENT_APPROVE),
                new TaskViewEvent("Ablehnen", EVENT_REJECT)
        );
    }

    @Nonnull
    @Override
    public AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context) throws ResponseException {
        return elementDataTransformService
                .buildEffectiveValues(
                        getStaffTaskView(context),
                        context.getThisTask().getProcessData()
                )
                .toAuthoredElementValues();
    }

    @Nullable
    @Override
    public AuthoredElementValues getAutoSavedStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context) {
        var savedData = ProcessNodeDefinition.super.getAutoSavedStaffTaskViewData(context);
        if (savedData != null) {
            return savedData;
        }

        var runtimeData = context.getThisTask().getRuntimeData();
        if (runtimeData.isEmpty()) {
            return null;
        }

        return ObjectMapperFactory
                .getNullPreservingInstance()
                .convertValue(runtimeData, AuthoredElementValues.class);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onAutoSaveFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context,
                                                                            @Nonnull AuthoredElementValues update) throws ResponseException, ProcessNodeExecutionException {
        return ProcessNodeDefinition.super.onAutoSaveFromStaffTaskView(context, update);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException {
        var remark = update.get(TASK_VIEW_REMARK_FIELD_ID);
        var remarkText = remark != null ? remark.toString() : null;

        final String port;
        final String decision;
        if (EVENT_APPROVE.equals(event)) {
            port = PORT_APPROVED;
            decision = PORT_APPROVED;
        } else if (EVENT_REJECT.equals(event)) {
            port = PORT_REJECTED;
            decision = PORT_REJECTED;
        } else {
            throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        }

        var nodeData = new HashMap<String, Object>();
        nodeData.put(OUTPUT_DECISION, decision);
        nodeData.put(OUTPUT_REMARK, remarkText);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getCallingUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, IsoTimestampUtils.nowUtc());

        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(port) // Set the desired output port
                .setNodeData(nodeData) // Set the generated node data
                .setRuntimeData(Map.of()) // Reset runtime data to empty map
                .setProcessData(context.getThisTask().getProcessData()); // Copy the process data for the next step

        return Optional.of(result);
    }

    @Nonnull
    private static ElementVisibilityFunctions buildModeVisibility(@Nonnull String expectedMode) {
        return ElementVisibilityFunctions
                .of(NoCodeExpression.of(
                        NoCodeEqualsOperator.OPERATOR_ID,
                        new NoCodeReference(CONTENT_MODE_FIELD_ID),
                        new NoCodeStaticValue(expectedMode)
                ))
                .recalculateReferencedIds();
    }

    @Nonnull
    private static Map<String, Object> extractWorkingProcessData(@Nonnull ProcessExecutionData processData)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        return new LinkedHashMap<>(processData.getProcessData());
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(ASSIGNMENT_CONTEXT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    @Override
    public Class<ApprovalConfiguration> getNodeConfigurationClass() {
        return ApprovalConfiguration.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class ApprovalConfiguration {
        public static final String CRITERIA = CRITERIA_FIELD_ID;
        @InputElementPOJOBinding(id = CRITERIA, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Freigabekriterien"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie die fachlichen Kriterien, auf deren Basis die Freigabe erfolgen soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String criteria;

        public static final String CONTENT_MODE = CONTENT_MODE_FIELD_ID;
        @InputElementPOJOBinding(id = CONTENT_MODE, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zu prüfende Inhalte"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob die Freigabe auf Basis modellierter Daten oder freier Inhalte erfolgt."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "toggleButtons", boolValue = true),
                @ElementPOJOBindingProperty(key = "displayInline", boolValue = true)
        })
        public String contentMode;

        public static final String DATA_CONTENT = DATA_CONTENT_FIELD_ID;
        @InputElementPOJOBinding(id = DATA_CONTENT, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zu prüfende Daten"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Modellieren Sie eine Gover-UI, in der die freizugebenden Inhalte dargestellt werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public GroupLayoutElement dataContent;

        public static final String CUSTOM_CONTENT = CUSTOM_CONTENT_FIELD_ID;
        @InputElementPOJOBinding(id = CUSTOM_CONTENT, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zu prüfende Inhalte"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie die zu prüfenden Inhalte frei, z. B. wenn diese in einem Drittsystem geprüft werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String customContent;

        public static final String ASSIGNMENT_CONTEXT = ASSIGNMENT_CONTEXT_FIELD_ID;
        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "headline", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "text", strValue = "Definieren Sie den Personenkreis, der für diese Aufgabe herangezogen werden kann."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Organisationseinheit, Team oder Mitarbeiter:in suchen"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }
}
