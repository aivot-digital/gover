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
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.*;
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
public class DataChangeActionNodeV1 implements ProcessNodeDefinition {
    public static final String NODE_KEY = "data_change";

    private static final String PORT_OUTPUT = "output";

    private static final String EVENT_SAVE = "save";
    private static final String EVENT_COMPLETE = "complete";

    private static final String RUNTIME_DATA_DRAFT_KEY = "draftData";
    private static final String DIFF_ROOT_ID = "__data_change_root__";
    private static final String DIFF_WRAPPER_KEY = "data";

    private static final String OUTPUT_DATA = "data";
    private static final String OUTPUT_DIFF = "diff";
    private static final String OUTPUT_PROCESSED_BY_USER_ID = "processedByUserId";
    private static final String OUTPUT_PROCESSED_AT = "processedAt";

    private static final String TASK_VIEW_ROOT_ID = "data-change-task-view";
    private static final String TASK_VIEW_DESCRIPTION_HEADLINE_ID = "data-change-task-view-description-headline";
    private static final String TASK_VIEW_DESCRIPTION_CONTENT_ID = "data-change-task-view-description-content";

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ElementDataTransformService elementDataTransformService;

    public DataChangeActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
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
        return "Daten ändern";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erlaubt einer Mitarbeiter:in, konfigurierte Vorgangsdaten in einer Gover-UI zu bearbeiten, zwischenzuspeichern und zu übernehmen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionContextConfig context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(DataChangeActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für den Datenänderungs-Knoten: %s",
                    e.getMessage()
            );
        }

        layout
                .findChild(DataChangeActionNodeConfig.DATA_DEFINITION_FIELD_ID, UiDefinitionInputElement.class)
                .ifPresent(element -> {
                    element.setElementType(ElementType.GroupLayout);
                    element.setDisplayContext(ElementDisplayContext.StaffFacing);
                });

        layout
                .findChild(DataChangeActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(element -> {
                    element.setHeadline("Verantwortlicher Personenkreis");
                    element.setText("Definieren Sie den Personenkreis, der diese Datenänderung vornehmen darf.");
                    element.setPlaceholder("Organisationseinheit, Team oder Mitarbeiter:in suchen");
                    element.setAllowedTypes(List.of("orgUnit", "team", "user"));
                    element.setProcessAccessConstraint(new de.aivot.GoverBackend.elements.models.elements.form.input.DomainAndUserSelectProcessAccessConstraint()
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
                        "Der Prozess wird hier fortgesetzt, nachdem die Datenänderung übernommen wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_DATA,
                        "Bearbeitete Daten",
                        "Die final übernommenen Daten aus der konfigurierten Gover-UI im Payload-Format."
                ),
                new ProcessNodeOutput(
                        OUTPUT_DIFF,
                        "Änderungen",
                        "Die Liste aller Änderungen zwischen den ursprünglichen und den übernommenen Vorgangsdaten."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_BY_USER_ID,
                        "Bearbeitet durch",
                        "Die ID der Mitarbeiter:in, die die Datenänderung übernommen hat."
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_AT,
                        "Bearbeitet am",
                        "Der Zeitstempel der finalen Übernahme im ISO-Format."
                )
        );
    }

    @Override
    public Map<String, String> validateConfiguration(@Nonnull de.aivot.GoverBackend.process.entities.ProcessNodeEntity processNodeEntity,
                                                     @Nonnull AuthoredElementValues configuration,
                                                     @Nonnull DerivedRuntimeElementData derivedRuntimeElementData) throws ResponseException {
        /* TODO: Fix this validation
        boolean hasErrors = false;

        try {
            resolveDataDefinition(derivedRuntimeElementData.getEffectiveValues().get(DataChangeActionNodeConfig.DATA_DEFINITION_FIELD_ID));
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            setValidationError(derivedRuntimeElementData, DataChangeActionNodeConfig.DATA_DEFINITION_FIELD_ID, e.getMessage());
            hasErrors = true;
        }

        try {
            resolveAssignmentContext(derivedRuntimeElementData.getEffectiveValues().get(DataChangeActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID));
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            setValidationError(derivedRuntimeElementData, DataChangeActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID, e.getMessage());
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
                new TaskViewEvent(
                        "Speichern",
                        EVENT_SAVE
                ),
                new TaskViewEvent(
                        "Speichern und abschließen",
                        EVENT_COMPLETE,
                        "outlined",
                        null,
                        "right"
                )
        );
    }

    @Nonnull
    @Override
    public AuthoredElementValues getStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff context) throws ResponseException {
        var config = loadConfigurationForUi(context);

        var initialData = elementDataTransformService
                .buildEffectiveValues(config.dataDefinition(), context.getThisTask().getProcessData())
                .toAuthoredElementValues();

        var draftData = readDraftData(context.getThisTask().getRuntimeData());
        if (draftData == null || draftData.isEmpty()) {
            return initialData;
        }

        var mergedData = new AuthoredElementValues();
        mergedData.putAll(initialData);
        mergedData.putAll(draftData);
        return mergedData;
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onUpdateFromStaff(@Nonnull ProcessNodeExecutionContextUIStaff context,
                                                                  @Nonnull AuthoredElementValues update,
                                                                  @Nonnull String event) throws ResponseException {
        var config = loadConfigurationForUi(context);

        return switch (event) {
            case EVENT_SAVE -> Optional.of(saveDraft(context, update));
            case EVENT_COMPLETE -> Optional.of(completeTask(context, config, update));
            default -> throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        };
    }

    @Nonnull
    private ProcessNodeExecutionResultTaskAssigned saveDraft(@Nonnull ProcessNodeExecutionContextUIStaff context,
                                                             @Nonnull AuthoredElementValues update) {
        var runtimeData = new LinkedHashMap<>(context.getThisTask().getRuntimeData());
        runtimeData.put(RUNTIME_DATA_DRAFT_KEY, copyAuthoredElementValues(update));

        var result = ProcessNodeExecutionResultTaskAssigned.of(resolveAssignedUserId(context));
        result.setRuntimeData(runtimeData);
        result.setNodeData(new LinkedHashMap<>(context.getThisTask().getNodeData()));
        result.setProcessData(context.getThisTask().getProcessData());
        return result;
    }

    @Nonnull
    private ProcessNodeExecutionResultTaskCompleted completeTask(@Nonnull ProcessNodeExecutionContextUIStaff context,
                                                                 @Nonnull ResolvedConfiguration config,
                                                                 @Nonnull AuthoredElementValues update) {
        var originalProcessData = ObjectMapperFactory.Utils.convertToMap(context.getThisTask().getProcessData());
        var payloadUpdate = elementDataTransformService.buildPayload(config.dataDefinition(), update);
        var updatedProcessData = mergeProcessData(originalProcessData, payloadUpdate);
        var diff = createProcessDataDiff(originalProcessData, updatedProcessData);

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_DATA, payloadUpdate);
        nodeData.put(OUTPUT_DIFF, diff);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, LocalDateTime.now().toString());

        var result = ProcessNodeExecutionResultTaskCompleted.of(PORT_OUTPUT);
        result.setProcessData(updatedProcessData);
        result.setNodeData(nodeData);
        result.setRuntimeData(Map.of());
        return result;
    }

    @Nonnull
    private static GroupLayoutElement buildStaffTaskView(@Nonnull ResolvedConfiguration config) {
        if (config.taskDescription() == null || config.taskDescription().isBlank()) {
            return cloneDataDefinition(config.dataDefinition());
        }

        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var descriptionHeadline = new HeadlineContentElement();
        descriptionHeadline.setId(TASK_VIEW_DESCRIPTION_HEADLINE_ID);
        descriptionHeadline.setContent("Aufgabenbeschreibung");

        var descriptionContent = new RichTextContentElement();
        descriptionContent.setId(TASK_VIEW_DESCRIPTION_CONTENT_ID);
        descriptionContent.setContent(config.taskDescription());

        var children = new java.util.ArrayList<BaseFormElement>();
        children.add(descriptionHeadline);
        children.add(descriptionContent);
        children.add(cloneDataDefinition(config.dataDefinition()));
        layout.setChildren(children);
        return layout;
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
        DataChangeActionNodeConfig config;
        try {
            var configuration = new EffectiveElementValues();
            configuration.putAll(rawConfiguration);
            config = ElementPOJOMapper.mapToPOJO(configuration, DataChangeActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die Konfiguration des Datenänderungs-Knotens ist ungültig: %s",
                    e.getMessage()
            );
        }

        return new ResolvedConfiguration(
                normalizeTaskDescription(config.taskDescription),
                resolveDataDefinition(config.dataDefinition),
                resolveAssignmentContext(config.assignmentContext)
        );
    }

    @Nullable
    private static String normalizeTaskDescription(@Nullable String taskDescription) {
        if (taskDescription == null || taskDescription.isBlank()) {
            return null;
        }

        return taskDescription;
    }

    @Nonnull
    private static GroupLayoutElement resolveDataDefinition(@Nullable Object rawDataDefinition)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (rawDataDefinition == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den Datenänderungs-Knoten muss eine Gover-UI definiert sein."
            );
        }

        final BaseElement element;
        try {
            element = ObjectMapperFactory
                    .getInstance()
                    .convertValue(rawDataDefinition, BaseElement.class);
        } catch (IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die konfigurierte Gover-UI ist ungültig: %s",
                    e.getMessage()
            );
        }

        if (!(element instanceof GroupLayoutElement dataDefinition)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die konfigurierte Gover-UI muss mit einer Gruppe beginnen."
            );
        }

        return dataDefinition;
    }

    @Nonnull
    private static AssignmentContextInputElementValue resolveAssignmentContext(@Nullable Object rawAssignmentContext)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var assignmentContext = AssignmentContextInputElement._formatValue(rawAssignmentContext);
        if (assignmentContext == null ||
                assignmentContext.getDomainAndUserSelection() == null ||
                assignmentContext.getDomainAndUserSelection().isEmpty()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Für den Datenänderungs-Knoten muss ein Personenkreis definiert sein."
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
            throw new IllegalStateException("Configured data-change UI is not a group layout.");
        }

        return copiedElement;
    }

    @Nullable
    private static AuthoredElementValues readDraftData(@Nonnull Map<String, Object> runtimeData) {
        var rawDraftData = runtimeData.get(RUNTIME_DATA_DRAFT_KEY);
        if (rawDraftData == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(rawDraftData, AuthoredElementValues.class);
    }

    @Nonnull
    private static AuthoredElementValues copyAuthoredElementValues(@Nonnull AuthoredElementValues source) {
        return ObjectMapperFactory
                .getInstance()
                .convertValue(source, AuthoredElementValues.class);
    }

    @Nonnull
    private static String resolveAssignedUserId(@Nonnull ProcessNodeExecutionContextUIStaff context) {
        var assignedUserId = context.getThisTask().getAssignedUserId();
        return assignedUserId != null ? assignedUserId : context.getUser().getId();
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
            @Nullable String taskDescription,
            @Nonnull GroupLayoutElement dataDefinition,
            @Nonnull AssignmentContextInputElementValue assignmentContext
    ) {
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DataChangeActionNodeConfig {
        public static final String TASK_DESCRIPTION_FIELD_ID = "task_description";
        public static final String DATA_DEFINITION_FIELD_ID = "data_definition";
        public static final String ASSIGNMENT_CONTEXT_FIELD_ID = "assignment_context";

        @InputElementPOJOBinding(id = TASK_DESCRIPTION_FIELD_ID, type = ElementType.RichTextInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Aufgabenbeschreibung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optionaler Hinweis für die später zugewiesene Mitarbeiter:in, was in dieser Aufgabe zu tun ist."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String taskDescription;

        @InputElementPOJOBinding(id = DATA_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bearbeitbare Daten"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Modellieren Sie eine Gover-UI, mit der die Mitarbeiter:in die Vorgangsdaten bearbeiten kann."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public Object dataDefinition;

        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der diese Aufgabe bearbeiten darf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public Object assignmentContext;
    }
}
