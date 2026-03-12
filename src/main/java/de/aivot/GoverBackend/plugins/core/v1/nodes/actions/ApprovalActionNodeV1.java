package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.enums.ValueFunctionType;
import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.ElementValueFunctions;
import de.aivot.GoverBackend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.GoverBackend.elements.models.elements.LayoutElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.SpacerContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectProcessAccessConstraint;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import de.aivot.GoverBackend.nocode.models.NoCodeReference;
import de.aivot.GoverBackend.nocode.models.NoCodeStaticValue;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinitionContextConfig;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextInit;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.ProcessNodeOutput;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.TaskViewEvent;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.services.AssignmentContextAssigneeResolverService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ApprovalActionNodeV1 implements ProcessNodeDefinition {
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

    public ApprovalActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService) {
        this.assigneeResolverService = assigneeResolverService;
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) {
        var layout = new ConfigLayoutElement();
        layout.setId(getKey() + "-config");

        var criteriaField = new RichTextInputElement();
        criteriaField.setId(CRITERIA_FIELD_ID);
        criteriaField.setLabel("Freigabekriterien");
        criteriaField.setHint("Beschreiben Sie die fachlichen Kriterien, auf deren Basis die Freigabe erfolgen soll.");
        criteriaField.setRequired(true);
        layout.addChild(criteriaField);

        var contentModeField = new RadioInputElement();
        contentModeField.setId(CONTENT_MODE_FIELD_ID);
        contentModeField.setLabel("Zu prüfende Inhalte");
        contentModeField.setHint("Wählen Sie, ob die Freigabe auf Basis modellierter Daten oder freier Inhalte erfolgt.");
        contentModeField.setRequired(true);
        contentModeField.setToggleButtons(true);
        contentModeField.setDisplayInline(true);
        contentModeField.setValue(new ElementValueFunctions()
                .setType(ValueFunctionType.NoCode)
                .setNoCode(new NoCodeStaticValue(MODE_DATA)));
        contentModeField.setOptions(List.of(
                RadioInputElementOption.of(MODE_DATA, "Daten"),
                RadioInputElementOption.of(MODE_CUSTOM_CONTENT, "Eigene Inhalte")
        ));
        layout.addChild(contentModeField);

        var dataContentField = new UiDefinitionInputElement();
        dataContentField.setId(DATA_CONTENT_FIELD_ID);
        dataContentField.setLabel("Zu prüfende Daten");
        dataContentField.setHint("Modellieren Sie eine Gover-UI, in der die freizugebenden Inhalte dargestellt werden.");
        dataContentField.setRequired(true);
        dataContentField.setElementType(ElementType.Group);
        dataContentField.setVisibility(buildModeVisibility(MODE_DATA));
        layout.addChild(dataContentField);

        var customContentField = new RichTextInputElement();
        customContentField.setId(CUSTOM_CONTENT_FIELD_ID);
        customContentField.setLabel("Zu prüfende Inhalte");
        customContentField.setHint("Beschreiben Sie die zu prüfenden Inhalte frei, z. B. wenn diese in einem Drittsystem geprüft werden.");
        customContentField.setRequired(true);
        customContentField.setReducedMode(false);
        customContentField.setVisibility(buildModeVisibility(MODE_CUSTOM_CONTENT));
        layout.addChild(customContentField);

        var assignmentContextField = new AssignmentContextInputElement();
        assignmentContextField.setId(ASSIGNMENT_CONTEXT_FIELD_ID);
        assignmentContextField.setLabel("Verantwortlicher Personenkreis");
        assignmentContextField.setRequired(true);
        assignmentContextField.setHeadline("Verantwortlicher Personenkreis");
        assignmentContextField.setText("Definieren Sie den Personenkreis, der für diese Aufgabe herangezogen werden kann.");
        assignmentContextField.setPlaceholder("Organisationseinheit, Team oder Mitarbeiter:in suchen");
        assignmentContextField.setAllowedTypes(List.of("orgUnit", "team", "user"));
        assignmentContextField.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                .setProcessId(context.processDefinition().getId())
                .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                .setRequiredPermissions(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)));
        layout.addChild(assignmentContextField);

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
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var config = loadConfiguration(context.getThisNode());
        var workingProcessData = extractWorkingProcessData(context.getProcessData());

        var assigneeUserId = assigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        config.assignmentContext(),
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für das Prozesselement '%s' konnte keine geeignete Bearbeiter:in im konfigurierten Personenkreis ermittelt werden.",
                        context.getThisNode().getName() != null ? context.getThisNode().getName() : getName()
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setProcessData(workingProcessData);
    }

    @Nonnull
    @Override
    public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        return buildStaffTaskView(loadConfigurationForUi(context));
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff context) {
        return List.of(
                new TaskViewEvent("Freigeben", EVENT_APPROVE),
                new TaskViewEvent("Ablehnen", EVENT_REJECT)
        );
    }

    @Nonnull
    @Override
    public ElementData getStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        var config = loadConfigurationForUi(context);
        var layout = buildStaffTaskView(config);

        var valueMap = new HashMap<String, Object>();
        if (context.getThisTask().getProcessData() != null) {
            valueMap.putAll(context.getThisTask().getProcessData());
        }

        if (context.getThisTask().getNodeData() != null &&
            context.getThisTask().getNodeData().containsKey(TASK_VIEW_REMARK_FIELD_ID)) {
            valueMap.put(TASK_VIEW_REMARK_FIELD_ID, context.getThisTask().getNodeData().get(TASK_VIEW_REMARK_FIELD_ID));
        }

        return ElementData.fromValueMap(layout, valueMap);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onUpdateFromStaff(
            @Nonnull ProcessNodeExecutionContextUIStaff context,
            @Nonnull Map<String, Object> update,
            @Nonnull String event
    ) throws ResponseException {
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
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, LocalDateTime.now().toString());
        nodeData.put(TASK_VIEW_REMARK_FIELD_ID, remarkText);

        var result = new ProcessNodeExecutionResultTaskCompleted();
        result.setViaPort(port);
        result.setNodeData(nodeData);
        result.setProcessData(context.getThisTask().getProcessData());
        return Optional.of(result);
    }

    @Nonnull
    private GroupLayoutElement buildStaffTaskView(@Nonnull ApprovalConfiguration config) {
        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var children = new ArrayList<BaseFormElement>();
        var criteriaHeadline = new HeadlineContentElement();
        criteriaHeadline.setId("approval-criteria-headline");
        criteriaHeadline.setContent("Freigabekriterien");
        children.add(criteriaHeadline);

        var criteriaContent = new RichTextContentElement();
        criteriaContent.setId("approval-criteria-content");
        criteriaContent.setContent(config.criteria());
        children.add(criteriaContent);

        var contentHeadline = new HeadlineContentElement();
        contentHeadline.setId("approval-content-headline");
        contentHeadline.setContent("Inhalte");
        children.add(contentHeadline);

        if (MODE_DATA.equals(config.contentMode())) {
            children.add(prepareReadonlyElement(config.dataContent()));
        } else {
            var customContent = new RichTextContentElement();
            customContent.setId("approval-custom-content");
            customContent.setContent(config.customContent());
            children.add(customContent);
        }

        var remarkField = new RichTextInputElement();
        remarkField.setId(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setLabel("Vermerk");
        remarkField.setHint("Optionaler Vermerk zur Freigabeentscheidung.");
        remarkField.setRequired(false);
        remarkField.setReducedMode(true);
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
    private ApprovalConfiguration loadConfigurationForUi(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        try {
            return loadConfiguration(context.getThisNode());
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e);
        }
    }

    @Nonnull
    private ApprovalConfiguration loadConfiguration(@Nonnull de.aivot.GoverBackend.process.entities.ProcessNodeEntity node)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var configuration = node.getConfiguration();

        var criteria = readString(configuration, CRITERIA_FIELD_ID);
        var contentMode = readString(configuration, CONTENT_MODE_FIELD_ID);
        if (contentMode == null || contentMode.isBlank()) {
            contentMode = MODE_DATA;
        }

        if (!MODE_DATA.equals(contentMode) && !MODE_CUSTOM_CONTENT.equals(contentMode)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Das Freigabe-Element ist mit einem ungültigen Inhaltsmodus '%s' konfiguriert.",
                    contentMode
            );
        }

        var dataContentRaw = configuration.getOpt(DATA_CONTENT_FIELD_ID)
                .flatMap(elementDataObject -> elementDataObject.getOptionalValue())
                .orElse(null);
        var dataContent = dataContentRaw != null
                ? ObjectMapperFactory.getInstance().convertValue(dataContentRaw, BaseElement.class)
                : null;

        var customContent = readString(configuration, CUSTOM_CONTENT_FIELD_ID);
        var assignmentContextRaw = configuration.getOpt(ASSIGNMENT_CONTEXT_FIELD_ID)
                .flatMap(elementDataObject -> elementDataObject.getOptionalValue())
                .orElse(null);
        var assignmentContext = AssignmentContextInputElement._formatValue(assignmentContextRaw);

        if (criteria == null || criteria.isBlank()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für das Freigabe-Element müssen Freigabekriterien definiert sein."
            );
        }

        if (MODE_DATA.equals(contentMode)) {
            if (!(dataContent instanceof BaseFormElement)) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Für den Modus 'Daten' muss eine darstellbare Gover-UI definiert sein."
                );
            }
        } else if (customContent == null || customContent.isBlank()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den Modus 'Eigene Inhalte' muss ein Inhaltstext definiert sein."
            );
        }

        if (assignmentContext == null ||
            assignmentContext.getDomainAndUserSelection() == null ||
            assignmentContext.getDomainAndUserSelection().isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für das Freigabe-Element muss ein Personenkreis definiert sein."
            );
        }

        return new ApprovalConfiguration(
                criteria,
                contentMode,
                (BaseFormElement) dataContent,
                customContent,
                assignmentContext
        );
    }

    @Nullable
    private static String readString(@Nonnull ElementData configuration, @Nonnull String fieldId) {
        return configuration.getOpt(fieldId)
                .flatMap(elementDataObject -> elementDataObject.getOptionalValue())
                .map(Object::toString)
                .orElse(null);
    }

    @Nonnull
    private static Map<String, Object> extractWorkingProcessData(@Nonnull Map<String, Object> processData)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var rawWorkingProcessData = processData.get("$");

        if (!(rawWorkingProcessData instanceof Map<?, ?> rawWorkingProcessDataMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdatenwurzel ($) ist kein Objekt."
            );
        }

        var workingProcessData = new HashMap<String, Object>();
        for (var entry : rawWorkingProcessDataMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                workingProcessData.put(key, entry.getValue());
            }
        }

        return workingProcessData;
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
    private static BaseFormElement prepareReadonlyElement(@Nonnull BaseFormElement rawElement) {
        var copy = ObjectMapperFactory
                .getInstance()
                .convertValue(rawElement, BaseElement.class);

        if (!(copy instanceof BaseFormElement copiedFormElement)) {
            throw new IllegalStateException("Configured approval content is not a form element.");
        }

        markInputsDisabled(copiedFormElement);
        return copiedFormElement;
    }

    private static void markInputsDisabled(@Nonnull BaseElement element) {
        if (element instanceof BaseInputElement<?> inputElement) {
            inputElement.setDisabled(true);
        }

        if (element instanceof LayoutElement<?> layoutElement && layoutElement.getChildren() != null) {
            for (var child : layoutElement.getChildren()) {
                if (child != null) {
                    markInputsDisabled(child);
                }
            }
        }
    }

    private record ApprovalConfiguration(
            @Nonnull String criteria,
            @Nonnull String contentMode,
            @Nullable BaseFormElement dataContent,
            @Nullable String customContent,
            @Nonnull AssignmentContextInputElementValue assignmentContext
    ) {
    }
}
