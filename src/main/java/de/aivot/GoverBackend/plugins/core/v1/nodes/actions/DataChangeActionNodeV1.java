package de.aivot.GoverBackend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.GoverBackend.elements.annotations.InputElementPOJOBinding;
import de.aivot.GoverBackend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.GoverBackend.elements.enums.ElementDisplayContext;
import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.models.*;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.BaseFormElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.content.SpacerContentElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.GoverBackend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.GoverBackend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.GoverBackend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.GoverBackend.elements.services.ElementDerivationLogger;
import de.aivot.GoverBackend.elements.services.ElementDerivationService;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.plugins.core.CorePlugin;
import de.aivot.GoverBackend.process.enums.ProcessNodeType;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.GoverBackend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.GoverBackend.process.models.ProcessNodeDefinition;
import de.aivot.GoverBackend.process.models.ProcessNodeOutput;
import de.aivot.GoverBackend.process.models.ProcessNodePort;
import de.aivot.GoverBackend.process.models.TaskViewEvent;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.GoverBackend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.GoverBackend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.GoverBackend.process.permissions.ProcessPermissionProvider;
import de.aivot.GoverBackend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.GoverBackend.services.DiffService;
import de.aivot.GoverBackend.submission.services.ElementDataTransformService;
import de.aivot.GoverBackend.utils.StringUtils;
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
public class DataChangeActionNodeV1 implements ProcessNodeDefinition<DataChangeActionNodeV1.DataChangeActionNodeConfig> {
    public static final String NODE_KEY = "data_change";

    private static final String PORT_OUTPUT = "output";

    private static final String EVENT_COMPLETE = "complete";

    private static final String DIFF_ROOT_ID = "__data_change_root__";
    private static final String DIFF_WRAPPER_KEY = "data";

    private static final String OUTPUT_DATA = "data";
    private static final String OUTPUT_DIFF = "diff";
    private static final String OUTPUT_REMARK = "remark";
    private static final String OUTPUT_PROCESSED_BY_USER_ID = "processedByUserId";
    private static final String OUTPUT_PROCESSED_AT = "processedAt";

    private static final String TASK_VIEW_ROOT_ID = "data-change-task-view";
    private static final String TASK_VIEW_DESCRIPTION_HEADLINE_ID = "data-change-task-view-description-headline";
    private static final String TASK_VIEW_DESCRIPTION_CONTENT_ID = "data-change-task-view-description-content";
    private static final String TASK_VIEW_UI_HEADLINE_ID = "data-change-task-view-ui-headline";
    private static final String TASK_VIEW_REMARK_SPACER_ID = "data-change-task-view-remark-spacer";
    private static final String TASK_VIEW_REMARK_FIELD_ID = "dataChangeRemark";

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ElementDataTransformService elementDataTransformService;
    private final ElementDerivationService elementDerivationService;

    public DataChangeActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
                                  ElementDataTransformService elementDataTransformService, ElementDerivationService elementDerivationService) {
        this.assigneeResolverService = assigneeResolverService;
        this.elementDataTransformService = elementDataTransformService;
        this.elementDerivationService = elementDerivationService;
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
        return "Datenanpassung";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erlaubt einer Mitarbeiter:in, Daten in einer Gover-UI zu ändern, hinzuzufügen oder zu entfernen.";
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
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
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(DataChangeActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID);
        return configuration;
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
                        OUTPUT_REMARK,
                        "Änderungsvermerk",
                        "Der optionale interne Vermerk zur vorgenommenen Datenanpassung."
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
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<DataChangeActionNodeConfig> context) throws ProcessNodeExecutionException {
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
    public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context) throws ResponseException {
        var config = context.getConfigurationOfExecutingNode();

        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var children = new java.util.ArrayList<BaseFormElement>();

        var uiHeadline = new HeadlineContentElement();
        uiHeadline.setId(TASK_VIEW_UI_HEADLINE_ID);
        uiHeadline.setContent("Daten zu dieser Aufgabe");
        children.add(uiHeadline);
        children.add(cloneDataDefinition(config.dataDefinition));

        var remarkSpacer = new SpacerContentElement();
        remarkSpacer.setId(TASK_VIEW_REMARK_SPACER_ID);
        remarkSpacer.setHeight("16");
        children.add(remarkSpacer);

        var remarkField = new RichTextInputElement();
        remarkField.setId(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setLabel("Änderungsvermerk");
        remarkField.setHint("Optionaler interner Vermerk zur vorgenommenen Datenanpassung.");
        remarkField.setRequired(false);
        remarkField.setWeight(6.0);
        children.add(remarkField);

        layout.setChildren(children);
        return layout;
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context) {
        return List.of(
                new TaskViewEvent(
                        "Aufgabe abschließen",
                        EVENT_COMPLETE
                )
        );
    }

    @Nonnull
    @Override
    public AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context) throws ResponseException {
        var config = context.getConfigurationOfExecutingNode();

        return elementDataTransformService
                .buildEffectiveValues(config.dataDefinition, context.getThisTask().getProcessData())
                .toAuthoredElementValues();
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException {
        var config = context.getConfigurationOfExecutingNode();

        var derivationRequest = new ElementDerivationRequest(
                config.dataDefinition,
                update,
                new ElementDerivationOptions()
        );
        var derivationLogger = new ElementDerivationLogger();
        var derivedRuntimeData = elementDerivationService
                .derive(derivationRequest, derivationLogger);

        return switch (event) {
            case EVENT_COMPLETE -> Optional.of(completeTask(context, config, derivedRuntimeData.getEffectiveValues(), update));
            default -> throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        };
    }

    @Nonnull
    private ProcessNodeExecutionResultTaskCompleted completeTask(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context,
                                                                 @Nonnull DataChangeActionNodeConfig config,
                                                                 @Nonnull EffectiveElementValues update,
                                                                 @Nonnull AuthoredElementValues authoredUpdate) {
        var payloadUpdate = elementDataTransformService.buildPayload(config.dataDefinition, update);
        var originalProcessData = ObjectMapperFactory.Utils.convertToMap(context.getThisTask().getProcessData());
        var updatedProcessData = mergeProcessData(originalProcessData, payloadUpdate);
        var diff = createProcessDataDiff(originalProcessData, updatedProcessData);
        var remark = normalizeRemark(authoredUpdate.get(TASK_VIEW_REMARK_FIELD_ID));

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_DATA, payloadUpdate);
        nodeData.put(OUTPUT_DIFF, diff);
        nodeData.put(OUTPUT_REMARK, remark);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getCallingUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, LocalDateTime.now().toString());

        var result = ProcessNodeExecutionResultTaskCompleted.of(PORT_OUTPUT);
        result.setProcessData(updatedProcessData);
        result.setNodeData(nodeData);
        result.setRuntimeData(Map.of());
        return result;
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
                .filter(diffItem -> !"id".equals(diffItem.field()))
                .map(diffItem -> {
                    if (diffItem.field().equals(DIFF_WRAPPER_KEY)) {
                        return new DiffItem(
                                "",
                                diffItem.oldValue(),
                                diffItem.newValue()
                        );
                    }

                    if (diffItem.field().startsWith(DIFF_WRAPPER_KEY + ".")) {
                        return new DiffItem(
                                diffItem.field().substring(DIFF_WRAPPER_KEY.length() + 1),
                                diffItem.oldValue(),
                                diffItem.newValue()
                        );
                    }

                    if (diffItem.field().startsWith(DIFF_WRAPPER_KEY + "[")) {
                        return new DiffItem(
                                diffItem.field().substring(DIFF_WRAPPER_KEY.length()),
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

    @Nullable
    private static String normalizeRemark(@Nullable Object rawRemark) {
        if (rawRemark == null) {
            return null;
        }

        var remark = rawRemark.toString();
        return remark.isBlank() ? null : remark;
    }

    @Nonnull
    @Override
    public Class<DataChangeActionNodeConfig> getNodeConfigurationClass() {
        return DataChangeActionNodeConfig.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class DataChangeActionNodeConfig {
        public static final String DATA_DEFINITION_FIELD_ID = "data_definition";
        public static final String ASSIGNMENT_CONTEXT_FIELD_ID = "assignment_context";

        @InputElementPOJOBinding(id = DATA_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Bearbeitbare Daten"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Modellieren Sie eine Gover-UI, mit der die Mitarbeiter:in die Vorgangsdaten bearbeiten kann."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public GroupLayoutElement dataDefinition;

        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der diese Aufgabe bearbeiten darf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }
}
