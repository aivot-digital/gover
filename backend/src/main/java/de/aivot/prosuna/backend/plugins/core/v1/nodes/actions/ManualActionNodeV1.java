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
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.content.SpacerContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.*;
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
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
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
public class ManualActionNodeV1 implements ProcessNodeDefinition<ManualActionNodeV1.ManualActionNodeConfig> {
    public static final String NODE_KEY = "manual_action";

    private static final String PORT_OUTPUT = "output";
    private static final String EVENT_COMPLETE = "complete";

    private static final String OUTPUT_DATA = "data";
    private static final String OUTPUT_REMARK = "remark";
    private static final String OUTPUT_PROCESSED_BY_USER_ID = "processedByUserId";
    private static final String OUTPUT_PROCESSED_AT = "processedAt";
    private static final String OUTPUT_UNMAPPED = "unmapped";

    private static final String TASK_VIEW_ROOT_ID = "manual-action-task-view";
    private static final String TASK_VIEW_DESCRIPTION_HEADLINE_ID = "manual-action-description-headline";
    private static final String TASK_VIEW_DESCRIPTION_CONTENT_ID = "manual-action-description-content";
    private static final String TASK_VIEW_UI_HEADLINE_ID = "manual-action-ui-headline";
    private static final String TASK_VIEW_REMARK_SPACER_ID = "manual-action-remark-spacer";
    private static final String TASK_VIEW_REMARK_FIELD_ID = "manualActionRemark";
    private static final String TASK_VIEW_ACTIONS_SPACER_ID = "manual-action-actions-spacer";

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ElementDataTransformService elementDataTransformService;
    private final ElementDerivationService elementDerivationService;
    private final TemplateRenderService templateRenderService;


    public ManualActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
                              ElementDataTransformService elementDataTransformService,
                              ElementDerivationService elementDerivationService,
                              TemplateRenderService templateRenderService) {
        this.assigneeResolverService = assigneeResolverService;
        this.elementDataTransformService = elementDataTransformService;
        this.elementDerivationService = elementDerivationService;
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
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
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
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(ManualActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    @Override
    public Class<ManualActionNodeConfig> getNodeConfigurationClass() {
        return ManualActionNodeConfig.class;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_OUTPUT,
                        "Aktion bestätigt",
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
                        "Die über die optionale Prosuna-UI bestätigten oder erfassten Daten im Payload-Format."
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
                                                     @Nonnull ManualActionNodeConfig configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        return ProcessNodeDefinitionMetadata
                .reuse(previousMetadata)
                .withLayout(configuration.uiDefinition, processNodeEntity);
    }

    @Override
    public Map<String, List<String>> validateConfiguration(@Nonnull ProcessNodeEntity processNodeEntity,
                                                           @Nonnull ManualActionNodeConfig configuration) throws ResponseException {
        return null;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<ManualActionNodeConfig> context) throws ProcessNodeExecutionException {
        var config = loadConfiguration(context.getConfigurationOfExecutingNode());
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
    public GroupLayoutElement getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context) throws ResponseException {
        return buildStaffTaskView(loadConfigurationForUi(context), context);
    }

    @Nonnull
    @Override
    public List<TaskViewEvent> getStaffTaskViewEvents(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context) {
        return List.of(
                new TaskViewEvent(
                        "Aufgabe abschließen",
                        EVENT_COMPLETE
                )
        );
    }

    @Nonnull
    @Override
    public AuthoredElementValues createDefaultStaffTaskViewData(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context) throws ResponseException {
        var config = loadConfigurationForUi(context);
        return config.uiDefinition() != null
                ? elementDataTransformService
                .buildEffectiveValues(config.uiDefinition(), context.getThisTask().getProcessData())
                .toAuthoredElementValues()
                : new AuthoredElementValues();
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException {
        var config = loadConfigurationForUi(context);
        var derivedUiUpdate = deriveUiUpdate(config, update);

        return switch (event) {
            case EVENT_COMPLETE -> Optional.of(completeTask(context, config, derivedUiUpdate, update));
            default -> throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        };
    }

    @Nonnull
    private GroupLayoutElement buildStaffTaskView(@Nonnull ResolvedConfiguration config,
                                                  @Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context) {
        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var descriptionHeadline = new HeadlineContentElement();
        descriptionHeadline.setId(TASK_VIEW_DESCRIPTION_HEADLINE_ID);
        descriptionHeadline.setContent("Aufgabenbeschreibung");

        var descriptionContent = new RichTextContentElement();
        descriptionContent.setId(TASK_VIEW_DESCRIPTION_CONTENT_ID);
        var renderedDescription = templateRenderService
                .interpolate(context.getCurrentProcessExecutionData(), config.taskDescription());
        descriptionContent.setContent(renderedDescription);

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
    private ResolvedConfiguration loadConfigurationForUi(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context) throws ResponseException {
        try {
            return loadConfiguration(context.getConfigurationOfExecutingNode());
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e);
        }
    }

    @Nonnull
    private ResolvedConfiguration loadConfiguration(@Nonnull ManualActionNodeConfig config)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
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
                    "Die konfigurierte Prosuna-UI der manuellen Aktion ist ungültig: %s",
                    e.getMessage()
            );
        }

        if (!(element instanceof GroupLayoutElement uiDefinition)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die konfigurierte Prosuna-UI der manuellen Aktion muss mit einer Gruppe beginnen."
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

    @Nonnull
    private DerivedRuntimeElementData deriveUiUpdate(@Nonnull ResolvedConfiguration config,
                                                     @Nonnull AuthoredElementValues update) {
        if (config.uiDefinition() == null) {
            return new DerivedRuntimeElementData();
        }

        var derivationRequest = new ElementDerivationRequest(
                config.uiDefinition(),
                update,
                new ElementDerivationOptions()
        );
        return elementDerivationService
                .derive(derivationRequest);
    }

    @Nonnull
    private ProcessNodeExecutionResultTaskCompleted completeTask(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context,
                                                                 @Nonnull ResolvedConfiguration config,
                                                                 @Nonnull DerivedRuntimeElementData derivedUiUpdate,
                                                                 @Nonnull AuthoredElementValues update) throws ResponseException {
        var effectiveUiUpdate = derivedUiUpdate.getEffectiveValues();
        var payloadUpdate = config.uiDefinition() != null
                ? elementDataTransformService.buildPayload(config.uiDefinition(), effectiveUiUpdate, derivedUiUpdate.getElementStates())
                : Map.<String, Object>of();
        var originalProcessData = ObjectMapperFactory.Utils.convertToMapPreservingNulls(context.getThisTask().getProcessData());
        var updatedProcessData = config.uiDefinition() != null
                ? elementDataTransformService.buildPayload(
                        config.uiDefinition(),
                        effectiveUiUpdate,
                        derivedUiUpdate.getElementStates(),
                        ObjectMapperFactory.Utils.convertToMapPreservingNulls(originalProcessData)
                )
                : ObjectMapperFactory.Utils.convertToMapPreservingNulls(originalProcessData);
        var remark = normalizeRemark(update.get(TASK_VIEW_REMARK_FIELD_ID));

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_DATA, payloadUpdate);
        nodeData.put(OUTPUT_REMARK, remark);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getCallingUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, Instant.now());
        nodeData.put(OUTPUT_UNMAPPED, effectiveUiUpdate);

        var result = ProcessNodeExecutionResultTaskCompleted.of(PORT_OUTPUT);
        result.setProcessData(updatedProcessData);
        result.setNodeData(nodeData);
        result.setRuntimeData(Map.of());
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
    private static Map<String, Object> extractWorkingProcessData(@Nonnull ProcessExecutionData processData)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        return new LinkedHashMap<>(processData.getProcessData());
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
                @ElementPOJOBindingProperty(key = "label", strValue = "Aufgabenbeschreibung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie die manuelle Handlung, die außerhalb des Systems ausgeführt und bestätigt werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String taskDescription;

        @InputElementPOJOBinding(id = UI_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Daten zu dieser Aufgabe"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Optional: Modellieren Sie eine Prosuna-UI, wenn zur Aufgabe Daten angezeigt oder erfasst werden sollen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public GroupLayoutElement uiDefinition;

        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie den Personenkreis, der diese Aufgabe bearbeiten darf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public Object assignmentContext;
    }
}
