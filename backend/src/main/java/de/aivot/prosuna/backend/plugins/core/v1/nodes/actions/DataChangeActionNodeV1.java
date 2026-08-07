package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.core.services.ObjectMapperFactory;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.ElementDisplayContext;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.ElementDerivationRequest;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.HeadlineContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.SpacerContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.DomainAndUserSelectProcessAccessConstraint;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.UiDefinitionInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.submission.services.ElementDataTransformService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DataChangeActionNodeV1 implements ProcessNodeDefinition<DataChangeActionNodeV1.DataChangeActionNodeConfig> {
    public static final String NODE_KEY = "data_change";

    private static final String PORT_OUTPUT = "output";

    private static final String EVENT_COMPLETE = "complete";

    private static final String OUTPUT_DATA = "data";
    private static final String OUTPUT_REMARK = "remark";
    private static final String OUTPUT_PROCESSED_BY_USER_ID = "processedByUserId";
    private static final String OUTPUT_PROCESSED_AT = "processedAt";
    private static final String OUTPUT_UNMAPPED = "unmapped";

    private static final String TASK_VIEW_ROOT_ID = "data-change-task-view";
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
                    element.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
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
                        "Datenänderung übernommen",
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
                ),
                new ProcessNodeOutput(
                        OUTPUT_UNMAPPED,
                        "Formular-Rohdaten",
                        "Enthält alle Formulardaten unter der jeweiligen Element-ID des Feldes, unabhängig davon, ob ein Element über einen Datenschlüssel zugewiesen wurde oder nicht."
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull DataChangeActionNodeConfig configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        return ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .withLayout(configuration.dataDefinition, processNodeEntity);
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
                        context.getThisNode().getId(),
                        context.getThisTask().getId(),
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
        var derivedRuntimeData = elementDerivationService
                .derive(derivationRequest);

        return switch (event) {
            case EVENT_COMPLETE -> Optional.of(completeTask(context, config, derivedRuntimeData, update));
            default -> throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        };
    }

    @Nonnull
    private ProcessNodeExecutionResultTaskCompleted completeTask(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context,
                                                                 @Nonnull DataChangeActionNodeConfig config,
                                                                 @Nonnull DerivedRuntimeElementData derivedRuntimeData,
                                                                 @Nonnull AuthoredElementValues authoredUpdate) throws ResponseException {
        var update = derivedRuntimeData.getEffectiveValues();
        var payloadUpdate = elementDataTransformService.buildPayload(
                config.dataDefinition,
                update,
                derivedRuntimeData.getElementStates()
        );
        var originalProcessData = ObjectMapperFactory.Utils.convertToMapPreservingNulls(context.getThisTask().getProcessData());
        var updatedProcessData = elementDataTransformService.buildPayload(
                config.dataDefinition,
                update,
                derivedRuntimeData.getElementStates(),
                ObjectMapperFactory.Utils.convertToMapPreservingNulls(originalProcessData)
        );
        var remark = normalizeRemark(authoredUpdate.get(TASK_VIEW_REMARK_FIELD_ID));

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_DATA, payloadUpdate);
        nodeData.put(OUTPUT_REMARK, remark);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getCallingUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, Instant.now());
        nodeData.put(OUTPUT_UNMAPPED, update);

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
