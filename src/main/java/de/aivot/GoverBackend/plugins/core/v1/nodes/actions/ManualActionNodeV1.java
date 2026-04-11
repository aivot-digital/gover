package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ElementDisplayContext;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.AuthoredElementValues;
import de.aivot.GoverBackend.elements.models.ComputedElementState;
import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.elements.models.EffectiveElementValues;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.SpacerContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectProcessAccessConstraint;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
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
import de.aivot.GoverBackend.services.DiffService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ManualActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "manual_action";

    private static final String PORT_OUTPUT = "output";
    private static final String EVENT_CONFIRM = "confirm";

    private static final String DIFF_ROOT_ID = "__manual_action_root__";
    private static final String DIFF_WRAPPER_KEY = "data";

    private static final String OUTPUT_DATA = "data";
    private static final String OUTPUT_DIFF = "diff";
    private static final String OUTPUT_REMARK = "remark";
    private static final String OUTPUT_PROCESSED_BY_USER_ID = "processedByUserId";
    private static final String OUTPUT_PROCESSED_AT = "processedAt";

    private static final String TASK_VIEW_ROOT_ID = "manual-action-task-view";
    private static final String TASK_VIEW_DESCRIPTION_HEADLINE_ID = "manual-action-description-headline";
    private static final String TASK_VIEW_DESCRIPTION_CONTENT_ID = "manual-action-description-content";
    private static final String TASK_VIEW_UI_HEADLINE_ID = "manual-action-ui-headline";
    private static final String TASK_VIEW_REMARK_SPACER_ID = "manual-action-remark-spacer";
    private static final String TASK_VIEW_REMARK_FIELD_ID = "manualActionRemark";
    private static final String TASK_VIEW_ACTIONS_SPACER_ID = "manual-action-actions-spacer";

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ElementDataTransformService elementDataTransformService;

    public ManualActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
                              ElementDataTransformService elementDataTransformService) {
        this.assigneeResolverService = assigneeResolverService;
        this.elementDataTransformService = elementDataTransformService;
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
        return "Manuelle Aktion ausführen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Eine frei definierte, manuelle Aufgabe, welche durch eine Mitarbeiter:in (z. B. in einem dritten System) ausgeführt wird.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(ManualActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für die manuelle Aktion: %s",
                    e.getMessage()
            );
        }

        layout
                .findChild(ManualActionNodeConfig.UI_DEFINITION_FIELD_ID, UiDefinitionInputElement.class)
                .ifPresent(element -> {
                    element.setElementType(ElementType.GroupLayout);
                    element.setDisplayContext(ElementDisplayContext.StaffFacing);
                });

        layout
                .findChild(ManualActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(element -> {
                    element.setHeadline("Verantwortlicher Personenkreis");
                    element.setText("Definieren Sie den Personenkreis, der diese manuelle Aktion bearbeiten und bestätigen darf.");
                    element.setPlaceholder("Organisationseinheit, Team oder Mitarbeiter:in suchen");
                    element.setAllowedTypes(List.of("orgUnit", "team", "user"));
                    element.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
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
                        PORT_OUTPUT,
                        "Weiter",
                        "Der Prozess wird hier fortgesetzt, nachdem die manuelle Aktion bestätigt wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_DATA,
                        "Erfasste Daten",
                        "Die über die optionale Gover-UI bestätigten oder erfassten Daten im Payload-Format."
                ),
                new ProcessNodeOutput(
                        OUTPUT_DIFF,
                        "Änderungen",
                        "Die Liste aller Änderungen zwischen den ursprünglichen und den bestätigten Vorgangsdaten."
                ),
                new ProcessNodeOutput(
                        OUTPUT_REMARK,
                        "Vermerk",
                        "Der optionale interne Vermerk zur bestätigten manuellen Aktion."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_BY_USER_ID,
                        "Bearbeitet durch",
                        "Die ID der Mitarbeiter:in, die die manuelle Aktion bestätigt hat."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_AT,
                        "Bearbeitet am",
                        "Der Zeitstempel der Bestätigung im ISO-Format."
                )
        );
    }

    @Override
    public Map<String, String> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull AuthoredElementValues configuration,
                                                     @Nonnull DerivedRuntimeElementData derivedRuntimeElementData) throws ResponseException {
       /* TODO: Fix this validation
        boolean hasErrors = false;

        try {
            resolveTaskDescription(derivedRuntimeElementData.getEffectiveValues().get(ManualActionNodeConfig.TASK_DESCRIPTION_FIELD_ID));
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            setValidationError(derivedRuntimeElementData, ManualActionNodeConfig.TASK_DESCRIPTION_FIELD_ID, e.getMessage());
            hasErrors = true;
        }

        try {
            resolveUiDefinition(derivedRuntimeElementData.getEffectiveValues().get(ManualActionNodeConfig.UI_DEFINITION_FIELD_ID));
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            setValidationError(derivedRuntimeElementData, ManualActionNodeConfig.UI_DEFINITION_FIELD_ID, e.getMessage());
            hasErrors = true;
        }

        try {
            resolveAssignmentContext(derivedRuntimeElementData.getEffectiveValues().get(ManualActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID));
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            setValidationError(derivedRuntimeElementData, ManualActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID, e.getMessage());
            hasErrors = true;
        }

        if (hasErrors) {
            throw ResponseException.badRequest(derivedRuntimeElementData);
        }
        */

        return null;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionContextInit context) throws ProcessNodeExecutionException {
        var config = loadConfiguration(context.getConfiguration().getEffectiveValues());
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
                .setProcessData(workingProcessData)
                .setRuntimeData(Map.of());
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
                new TaskViewEvent("Aufgabe bestätigen", EVENT_CONFIRM)
        );
    }

    @Nonnull
    @Override
    public AuthoredElementValues getStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        var config = loadConfigurationForUi(context);
        if (config.uiDefinition() == null) {
            return new AuthoredElementValues();
        }

        return elementDataTransformService
                .buildEffectiveValues(config.uiDefinition(), context.getThisTask().getProcessData())
                .toAuthoredElementValues();
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onUpdateFromStaff(@Nonnull ProcessNodeExecutionContextUIStaff context,
                                                                  @Nonnull AuthoredElementValues update,
                                                                  @Nonnull String event) throws ResponseException {
        if (!EVENT_CONFIRM.equals(event)) {
            throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        }

        var config = loadConfigurationForUi(context);

        var originalProcessData = ObjectMapperFactory.Utils.convertToMap(context.getThisTask().getProcessData());
        var payloadUpdate = config.uiDefinition() != null
                ? elementDataTransformService.buildPayload(config.uiDefinition(), update)
                : Map.<String, Object>of();
        var updatedProcessData = mergeProcessData(originalProcessData, payloadUpdate);
        var diff = createProcessDataDiff(originalProcessData, updatedProcessData);
        var remark = normalizeRemark(update.get(TASK_VIEW_REMARK_FIELD_ID));

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_DATA, payloadUpdate);
        nodeData.put(OUTPUT_DIFF, diff);
        nodeData.put(OUTPUT_REMARK, remark);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, LocalDateTime.now().toString());

        var result = ProcessNodeExecutionResultTaskCompleted.of(PORT_OUTPUT);
        result.setProcessData(updatedProcessData);
        result.setNodeData(nodeData);
        result.setRuntimeData(Map.of());
        return Optional.of(result);
    }

    @Nonnull
    private static GroupLayoutElement buildStaffTaskView(@Nonnull ResolvedConfiguration config) {
        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var descriptionHeadline = new HeadlineContentElement();
        descriptionHeadline.setId(TASK_VIEW_DESCRIPTION_HEADLINE_ID);
        descriptionHeadline.setContent("Beschreibung der Aufgabe");

        var descriptionContent = new RichTextContentElement();
        descriptionContent.setId(TASK_VIEW_DESCRIPTION_CONTENT_ID);
        descriptionContent.setContent(config.taskDescription());

        var children = new java.util.ArrayList<BaseFormElement>();
        children.add(descriptionHeadline);
        children.add(descriptionContent);

        if (config.uiDefinition() != null) {
            var uiHeadline = new HeadlineContentElement();
            uiHeadline.setId(TASK_VIEW_UI_HEADLINE_ID);
            uiHeadline.setContent("Daten zu dieser Aufgabe");
            children.add(uiHeadline);
            children.add(cloneDataDefinition(config.uiDefinition()));
        }

        var remarkSpacer = new SpacerContentElement();
        remarkSpacer.setId(TASK_VIEW_REMARK_SPACER_ID);
        remarkSpacer.setHeight("16");
        children.add(remarkSpacer);

        var remarkField = new RichTextInputElement();
        remarkField.setId(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setLabel("Vermerk");
        remarkField.setHint("Optionaler interner Vermerk zur durchgeführten manuellen Aktion.");
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
    private ResolvedConfiguration loadConfigurationForUi(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        try {
            return loadConfiguration(context.getRuntimeElementData().getEffectiveValues());
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e);
        }
    }

    @Nonnull
    private ResolvedConfiguration loadConfiguration(@Nonnull Map<String, Object> rawConfiguration)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        ManualActionNodeConfig config;
        try {
            var configuration = new EffectiveElementValues();
            configuration.putAll(rawConfiguration);
            config = ElementPOJOMapper.mapToPOJO(configuration, ManualActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des Knotens für die manuelle Aktion ist ungültig: %s",
                    e.getMessage()
            );
        }

        return new ResolvedConfiguration(
                resolveTaskDescription(config.taskDescription),
                resolveUiDefinition(config.uiDefinition),
                resolveAssignmentContext(config.assignmentContext)
        );
    }

    @Nonnull
    private static String resolveTaskDescription(@Nullable Object rawTaskDescription)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawTaskDescription == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für die manuelle Aktion muss eine Aufgabenbeschreibung definiert sein."
            );
        }

        var taskDescription = rawTaskDescription.toString();
        if (taskDescription.isBlank()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für die manuelle Aktion muss eine Aufgabenbeschreibung definiert sein."
            );
        }

        return taskDescription;
    }

    @Nullable
    private static GroupLayoutElement resolveUiDefinition(@Nullable Object rawUiDefinition)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawUiDefinition == null) {
            return null;
        }

        final BaseElement element;
        try {
            element = ObjectMapperFactory
                    .getInstance()
                    .convertValue(rawUiDefinition, BaseElement.class);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die konfigurierte Gover-UI der manuellen Aktion ist ungültig: %s",
                    e.getMessage()
            );
        }

        if (!(element instanceof GroupLayoutElement uiDefinition)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die konfigurierte Gover-UI der manuellen Aktion muss mit einer Gruppe beginnen."
            );
        }

        return uiDefinition;
    }

    @Nonnull
    private static AssignmentContextInputElementValue resolveAssignmentContext(@Nullable Object rawAssignmentContext)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var assignmentContext = AssignmentContextInputElement._formatValue(rawAssignmentContext);
        if (assignmentContext == null ||
                assignmentContext.getDomainAndUserSelection() == null ||
                assignmentContext.getDomainAndUserSelection().isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für die manuelle Aktion muss ein Personenkreis definiert sein."
            );
        }

        return assignmentContext;
    }

    @Nonnull
    private static GroupLayoutElement cloneDataDefinition(@Nonnull GroupLayoutElement rawElement) {
        var copy = ObjectMapperFactory
                .getInstance()
                .convertValue(rawElement, BaseElement.class);

        if (!(copy instanceof GroupLayoutElement copiedElement)) {
            throw new IllegalStateException("Configured manual-action UI is not a group layout.");
        }

        return copiedElement;
    }

    @Nullable
    private static String normalizeRemark(@Nullable Object rawRemark) {
        if (rawRemark == null) {
            return null;
        }

        var remark = rawRemark.toString();
        return remark.isBlank() ? null : remark;
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

        var workingProcessData = new LinkedHashMap<String, Object>();
        for (var entry : rawWorkingProcessDataMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                workingProcessData.put(key, entry.getValue());
            }
        }

        return workingProcessData;
    }

    @Nonnull
    private static Map<String, Object> mergeProcessData(@Nonnull Map<String, Object> originalProcessData,
                                                        @Nonnull Map<String, Object> payloadUpdate) {
        var mergedProcessData = ObjectMapperFactory.Utils.convertToMap(originalProcessData);
        mergeInto(mergedProcessData, payloadUpdate);
        return mergedProcessData;
    }

    private static void mergeInto(@Nonnull Map<String, Object> target, @Nonnull Map<String, Object> patch) {
        for (var entry : patch.entrySet()) {
            var key = entry.getKey();
            var patchValue = entry.getValue();
            var targetValue = target.get(key);

            if (patchValue instanceof Map<?, ?> patchMap && targetValue instanceof Map<?, ?> targetMap) {
                var targetMapValue = castStringObjectMap(targetMap);
                mergeInto(targetMapValue, castStringObjectMap(patchMap));
                target.put(key, targetMapValue);
            } else if (patchValue instanceof Map<?, ?> patchMap) {
                target.put(key, castStringObjectMap(patchMap));
            } else {
                target.put(key, patchValue);
            }
        }
    }

    @Nonnull
    private static List<DiffItem> createProcessDataDiff(@Nonnull Map<String, Object> originalProcessData,
                                                        @Nonnull Map<String, Object> updatedProcessData) {
        var originalForDiff = Map.<String, Object>of(
                "id", DIFF_ROOT_ID,
                DIFF_WRAPPER_KEY, originalProcessData
        );
        var updatedForDiff = Map.<String, Object>of(
                "id", DIFF_ROOT_ID,
                DIFF_WRAPPER_KEY, updatedProcessData
        );

        return DiffService
                .createDiff(new JSONObject(originalForDiff), new JSONObject(updatedForDiff))
                .stream()
                .filter(diffItem -> !"/id".equals(diffItem.field()))
                .map(diffItem -> {
                    if (diffItem.field().startsWith("/" + DIFF_WRAPPER_KEY)) {
                        return new DiffItem(
                                diffItem.field().substring(DIFF_WRAPPER_KEY.length() + 1),
                                diffItem.oldValue(),
                                diffItem.newValue()
                        );
                    }

                    return diffItem;
                })
                .toList();
    }

    @Nonnull
    private static Map<String, Object> castStringObjectMap(@Nonnull Object rawMap) {
        var result = new LinkedHashMap<String, Object>();
        if (rawMap instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    result.put(key, entry.getValue());
                }
            }
        }
        return result;
    }

    private static void setValidationError(@Nonnull DerivedRuntimeElementData derivedRuntimeElementData,
                                           @Nonnull String fieldId,
                                           @Nonnull String error) {
        derivedRuntimeElementData
                .getElementStates()
                .computeIfAbsent(fieldId, ignored -> new ComputedElementState())
                .setError(error);
    }

    private record ResolvedConfiguration(
            @Nonnull String taskDescription,
            @Nullable GroupLayoutElement uiDefinition,
            @Nonnull AssignmentContextInputElementValue assignmentContext
    ) {
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class ManualActionNodeConfig {
        public static final String TASK_DESCRIPTION_FIELD_ID = "task_description";
        public static final String UI_DEFINITION_FIELD_ID = "ui_definition";
        public static final String ASSIGNMENT_CONTEXT_FIELD_ID = "assignment_context";

        @InputElementPOJOBinding(id = TASK_DESCRIPTION_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Beschreibung der Aufgabe"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie die manuelle Handlung, die außerhalb des Systems ausgeführt und bestätigt werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String taskDescription;

        @InputElementPOJOBinding(id = UI_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Gover-UI"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optional: Modellieren Sie eine Gover-UI, wenn zur Aufgabe Daten angezeigt oder erfasst werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public Object uiDefinition;

        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der diese Aufgabe bearbeiten darf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public Object assignmentContext;
    }
}
