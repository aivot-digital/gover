package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.SelectInputPresentation;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
import de.aivot.prosuna.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodeStaffView;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultInstanceAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeConfigurationValidationContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessInstancePermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessAssignmentService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class InstanceAssignmentActionNodeV1 implements ProcessNodeDefinition<InstanceAssignmentActionNodeV1.Config> {
    public static final String NODE_KEY = "assign_instance";

    private static final String PORT_SUCCESS = "success";
    private static final String OUTPUT_ASSIGNED_USER_ID = "assignedUserId";
    private static final String MODE_AUTOMATIC = "automatic";
    private static final String MODE_MANUAL = "manual";
    private static final String EVENT_ASSIGN = "assign";
    private static final String TASK_VIEW_ID = "instance-assignment-task-view";
    private static final String TASK_DESCRIPTION_HEADLINE_ID = "instance-assignment-description-headline";
    private static final String TASK_DESCRIPTION_CONTENT_ID = "instance-assignment-description-content";
    private static final String TASK_ASSIGNEE_FIELD_ID = "assignedUserId";

    private static final List<String> INSTANCE_RECIPIENT_PERMISSIONS = List.of(
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
    );
    private static final List<String> MANUAL_TASK_PERMISSIONS = List.of(
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK,
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_REASSIGN
    );

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ProcessAssignmentService assignmentService;

    public InstanceAssignmentActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
                                          ProcessAssignmentService assignmentService) {
        this.assigneeResolverService = assigneeResolverService;
        this.assignmentService = assignmentService;
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return CorePlugin.PLUGIN_KEY;
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
    public String getName() {
        return "Vorgang zuweisen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Weist einen Vorgang automatisch oder durch eine manuelle Aufgabe einer Person zu.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                Wählen Sie, ob eine Person automatisch aus einem konfigurierten Personenkreis ermittelt wird oder ob eine manuelle Zuweisungsaufgabe entsteht.
                Bei der manuellen Zuweisung bestimmt ein eigener Personenkreis, wer die Aufgabe erhält. Die bearbeitende Person wählt anschließend aus den aktuell für den Vorgang berechtigten Personen.
                Eine bestehende Zuweisung wird ersetzt. Nach erfolgreicher Zuweisung wird der Prozess fortgesetzt und die ID der zugewiesenen Person als Elementausgang bereitgestellt.
                """;
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public ProcessNodeExecutionType[] getExecutionTypes() {
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Automatic, ProcessNodeExecutionType.Manual};
    }

    @Nonnull
    @Override
    public AuthoredElementValues getInitialConfiguration() {
        return new AuthoredElementValues().putLiteral(Config.MODE_FIELD_ID, MODE_AUTOMATIC);
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(Config.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Konfiguration für die Vorgangszuweisung konnte nicht erstellt werden.");
        }

        layout.findChild(Config.MODE_FIELD_ID, RadioInputElement.class)
                .ifPresent(field -> field.setOptions(List.of(
                        RadioInputElementOption.of(MODE_AUTOMATIC, "Automatisch"),
                        RadioInputElementOption.of(MODE_MANUAL, "Manuell")
                )));

        layout.findChild(Config.TASK_DESCRIPTION_FIELD_ID, RichTextInputElement.class)
                .ifPresent(field -> field.setVisibility(ElementVisibilityFunctions.of(NoCodeExpression.of(
                        NoCodeEqualsOperator.OPERATOR_ID,
                        new NoCodeReference(Config.MODE_FIELD_ID),
                        new NoCodeStaticValue(MODE_MANUAL)
                )).recalculateReferencedIds()));

        layout
                .findChild(Config.AUTOMATIC_CONTEXT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(field -> {
                    field.setAllowedTypes(List.of(AssignmentContextInputElement.ALLOWED_TYPE_USER));
                    field.setDisableProcessInstanceAssigneeOption(true);
                    field.setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);

                    field.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(context.processDefinition().getId())
                            .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                            .setRequiredPermissions(INSTANCE_RECIPIENT_PERMISSIONS));

                    field.setVisibility(ElementVisibilityFunctions.of(NoCodeExpression.of(
                            NoCodeEqualsOperator.OPERATOR_ID,
                            new NoCodeReference(Config.MODE_FIELD_ID),
                            new NoCodeStaticValue(MODE_AUTOMATIC)
                    )).recalculateReferencedIds());
                });

        layout
                .findChild(Config.MANUAL_CONTEXT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(field -> {
                    field.setAllowedTypes(List.of(
                            AssignmentContextInputElement.ALLOWED_TYPE_ORG_UNIT,
                            AssignmentContextInputElement.ALLOWED_TYPE_TEAM,
                            AssignmentContextInputElement.ALLOWED_TYPE_USER
                    ));

                    field.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(context.processDefinition().getId())
                            .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                            .setRequiredPermissions(MANUAL_TASK_PERMISSIONS));

                    field.setVisibility(ElementVisibilityFunctions.of(NoCodeExpression.of(
                            NoCodeEqualsOperator.OPERATOR_ID,
                            new NoCodeReference(Config.MODE_FIELD_ID),
                            new NoCodeStaticValue(MODE_MANUAL)
                    )).recalculateReferencedIds());
                });

        return layout;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(Config.AUTOMATIC_CONTEXT_FIELD_ID);
        configuration.remove(Config.MANUAL_CONTEXT_FIELD_ID);
        return configuration;
    }

    @Nullable
    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeConfigurationValidationContext<Config> context) {
        var config = context.configuration();
        var mode = normalizeMode(config.mode);
        if (mode == null) {
            return Map.of(Config.MODE_FIELD_ID, List.of("Wählen Sie eine gültige Art der Vorgangszuweisung aus."));
        }

        var fieldId = MODE_AUTOMATIC.equals(mode)
                ? Config.AUTOMATIC_CONTEXT_FIELD_ID : Config.MANUAL_CONTEXT_FIELD_ID;
        var rawContext = MODE_AUTOMATIC.equals(mode)
                ? config.automaticAssignmentContext : config.manualAssignmentContext;
        var assignmentContext = AssignmentContextInputElement._formatValue(rawContext);
        if (assignmentContext == null || assignmentContext.getDomainAndUserSelection() == null
                || assignmentContext.getDomainAndUserSelection().isEmpty()) {
            return Map.of(fieldId, List.of("Wählen Sie einen Personenkreis aus."));
        }
        try {
            restrictedAssignmentContextElement().performValidation(assignmentContext);
        } catch (ValidationException e) {
            return Map.of(fieldId, List.of(e.getMessage()));
        }
        return null;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(new ProcessNodePort(PORT_SUCCESS, "Erfolgreich", "Der Vorgang wurde einer Person zugewiesen."));
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(new ProcessNodeOutput(OUTPUT_ASSIGNED_USER_ID, "Zugewiesene Person",
                "Die ID der Person, der der Vorgang zugewiesen wurde.", "string"));
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<Config> context) throws ProcessNodeExecutionException {
        var config = context.getConfigurationOfExecutingNode();
        var mode = requireMode(config.mode);
        var assignmentContext = requireAssignmentContext(MODE_AUTOMATIC.equals(mode)
                ? config.automaticAssignmentContext : config.manualAssignmentContext);
        var instanceId = context.getThisProcessInstance().getId();
        var permissions = MODE_AUTOMATIC.equals(mode) ? INSTANCE_RECIPIENT_PERMISSIONS : MANUAL_TASK_PERMISSIONS;

        Set<String> eligibleUserIds;
        try {
            eligibleUserIds = assignmentService.runtimeInstanceOptions(
                            instanceId,
                            MODE_AUTOMATIC.equals(mode) ? List.of() : MANUAL_TASK_PERMISSIONS
                    ).stream()
                    .map(option -> option.id())
                    .collect(Collectors.toSet());
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionInvalidAssignment(e,
                    "Die berechtigten Personen für die Vorgangszuweisung konnten nicht ermittelt werden.");
        }

        var selectedUserId = assigneeResolverService.resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        instanceId,
                        context.getThisNode().getId(),
                        context.getThisTask().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        assignmentContext,
                        permissions,
                        eligibleUserIds
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        MODE_AUTOMATIC.equals(mode)
                                ? "Im konfigurierten Personenkreis konnte keine geeignete Person für die Vorgangszuweisung gefunden werden."
                                : "Im konfigurierten Personenkreis konnte keine geeignete Person für die manuelle Zuweisungsaufgabe gefunden werden."
                ));

        if (MODE_MANUAL.equals(mode)) {
            return ProcessNodeExecutionResultTaskAssigned.of(selectedUserId)
                    .setProcessData(context.getCurrentProcessExecutionData().getProcessData())
                    .setRuntimeData(Map.of());
        }

        return assignmentResult(selectedUserId, context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context) throws ResponseException {
        if (!MODE_MANUAL.equals(normalizeMode(context.getConfigurationOfExecutingNode().mode))) {
            throw ResponseException.badRequest("Für diese Vorgangszuweisung gibt es keine manuelle Aufgabe.");
        }
        requireAssignedTaskUser(context);

        var options = assignmentService.instanceOptions(context.getCallingUser().getId(),
                context.getThisProcessInstance().getId());
        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ID);
        var children = new ArrayList<BaseFormElement>();

        var taskDescription = context.getConfigurationOfExecutingNode().taskDescription;
        if (taskDescription != null && !taskDescription.isBlank()) {
            var descriptionHeadline = new HeadlineContentElement();
            descriptionHeadline.setId(TASK_DESCRIPTION_HEADLINE_ID);
            descriptionHeadline.setContent("Aufgabenbeschreibung");
            children.add(descriptionHeadline);

            var descriptionContent = new RichTextContentElement();
            descriptionContent.setId(TASK_DESCRIPTION_CONTENT_ID);
            descriptionContent.setContent(taskDescription);
            children.add(descriptionContent);
        }

        var assigneeField = new SelectInputElement();
        assigneeField.setId(TASK_ASSIGNEE_FIELD_ID);
        assigneeField.setLabel("Zuständige Person");
        assigneeField.setHint(options.isEmpty()
                ? "Derzeit steht keine berechtigte Person für die Vorgangszuweisung zur Auswahl."
                : "Wählen Sie die Person aus, die künftig für den Vorgang zuständig sein soll.");
        assigneeField.setRequired(true);
        assigneeField.setDisabled(options.isEmpty());
        assigneeField.setOptions(options.stream()
                .map(option -> SelectInputElementOption.of(option.id(), option.name()))
                .toList());
        assigneeField.setPresentation(SelectInputPresentation.Combobox);
        children.add(assigneeField);
        layout.setChildren(children);

        return ProcessNodeStaffView.of(context, layout,
                options.isEmpty() ? List.of() : List.of(new TaskViewEvent("Vorgang zuweisen", EVENT_ASSIGN)),
                new AuthoredElementValues());
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException {
        if (!EVENT_ASSIGN.equals(event)
                || !MODE_MANUAL.equals(normalizeMode(context.getConfigurationOfExecutingNode().mode))) {
            throw ResponseException.badRequest("Unbekannte Aktion für die Vorgangszuweisung.");
        }
        requireAssignedTaskUser(context);

        var selectedUserId = StringUtils.toNullableTrimmedString(update.getLiteral(TASK_ASSIGNEE_FIELD_ID));
        if (selectedUserId == null) {
            throw ResponseException.badRequest("Wählen Sie eine zuständige Person aus.");
        }

        var validOption = assignmentService.instanceOptions(context.getCallingUser().getId(),
                        context.getThisProcessInstance().getId())
                .stream()
                .anyMatch(option -> option.id().equals(selectedUserId));
        if (!validOption) {
            throw ResponseException.badRequest("Die ausgewählte Person kann diesem Vorgang nicht zugewiesen werden. Wählen Sie eine andere Person aus.");
        }

        return Optional.of(assignmentResult(selectedUserId, context.getThisTask().getProcessData()));
    }

    @Nonnull
    private static ProcessNodeExecutionResultInstanceAssigned assignmentResult(@Nonnull String userId,
                                                                               @Nonnull Map<String, Object> processData) {
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_ASSIGNED_USER_ID, userId);
        var result = ProcessNodeExecutionResultInstanceAssigned.assignAndContinue(userId, PORT_SUCCESS);
        result.setNodeData(nodeData);
        result.setProcessData(processData);
        result.setRuntimeData(Map.of());
        return result;
    }

    @Nonnull
    private static String requireMode(@Nullable String rawMode) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var mode = normalizeMode(rawMode);
        if (mode == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie eine gültige Art der Vorgangszuweisung aus.");
        }
        return mode;
    }

    @Nullable
    private static String normalizeMode(@Nullable String rawMode) {
        if (rawMode == null) {
            return MODE_AUTOMATIC;
        }
        return MODE_AUTOMATIC.equals(rawMode) || MODE_MANUAL.equals(rawMode) ? rawMode : null;
    }

    @Nonnull
    private static AssignmentContextInputElementValue requireAssignmentContext(@Nullable Object rawContext)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var value = AssignmentContextInputElement._formatValue(rawContext);
        if (value == null || value.getDomainAndUserSelection() == null || value.getDomainAndUserSelection().isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie einen Personenkreis für die Vorgangszuweisung aus.");
        }
        try {
            restrictedAssignmentContextElement().performValidation(value);
        } catch (ValidationException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e.getMessage());
        }
        return value;
    }

    @Nonnull
    private static AssignmentContextInputElement restrictedAssignmentContextElement() {
        return new AssignmentContextInputElement()
                .setAllowedTypes(List.of(AssignmentContextInputElement.ALLOWED_TYPE_USER))
                .setDisableProcessInstanceAssigneeOption(true)
                .setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);
    }

    private static void requireAssignedTaskUser(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context)
            throws ResponseException {
        if (!context.getCallingUser().getId().equals(context.getThisTask().getAssignedUserId())) {
            throw ResponseException.forbidden();
        }
    }

    @Nonnull
    @Override
    public Class<Config> getNodeConfigurationClass() {
        return Config.class;
    }

    /**
     * Configuration for automatic recipient selection or a manual assignment task.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class Config {
        public static final String MODE_FIELD_ID = "mode";
        public static final String AUTOMATIC_CONTEXT_FIELD_ID = "automaticAssignmentContext";
        public static final String TASK_DESCRIPTION_FIELD_ID = "taskDescription";
        public static final String MANUAL_CONTEXT_FIELD_ID = "manualAssignmentContext";

        /**
         * Selects the execution mode; an absent value defaults to automatic execution.
         */
        @InputElementPOJOBinding(id = MODE_FIELD_ID, type = ElementType.Radio, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Art der Zuweisung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob die zuzuweisende Person automatisch ermittelt oder in einer Aufgabe ausgewählt wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String mode;

        /**
         * Candidate circle used only in automatic mode. An empty circle prevents execution.
         */
        @Nullable
        @InputElementPOJOBinding(id = AUTOMATIC_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Personenkreis für die Vorgangszuweisung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, aus dem die zugewiesene Person ermittelt wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue automaticAssignmentContext;

        /**
         * Optional rich-text instructions shown only in the manual staff task view.
         */
        @Nullable
        @InputElementPOJOBinding(id = TASK_DESCRIPTION_FIELD_ID, type = ElementType.RichTextInput,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Aufgabenbeschreibung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie die Auswahlkriterien für die dem Vorgang zuzuweisende Person."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String taskDescription;

        /**
         * Candidate circle for assigning the manual task; recipients are selected separately in the task view.
         */
        @Nullable
        @InputElementPOJOBinding(id = MANUAL_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der diese Aufgabe bearbeiten darf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue manualAssignmentContext;
    }
}
