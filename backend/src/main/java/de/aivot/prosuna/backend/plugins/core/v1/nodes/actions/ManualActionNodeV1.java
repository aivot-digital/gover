package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.ElementDisplayContext;
import de.aivot.prosuna.backend.elements.enums.InputMode;
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
import de.aivot.prosuna.backend.elements.services.AuthoredInputValueService;
import de.aivot.prosuna.backend.elements.services.ElementDerivationService;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
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
import tools.jackson.core.JacksonException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;

@Component
public class ManualActionNodeV1 implements ProcessNodeDefinition<ManualActionNodeV1.ManualActionNodeConfig> {
    private static final ProcessNodeDeadlineDefinition PROCESSING_DEADLINE = new ProcessNodeDeadlineDefinition(
            "processing", "Bearbeitung",
            "Zeit für die Bearbeitung und Bestätigung der freien Aufgabe. Die Frist beginnt mit der Aufgabenzuweisung.",
            new ProcessNodePort("processing_overdue", "Fristüberschreitung: Bearbeitung",
                    "Der Vorgang wird hier fortgesetzt, wenn die Aufgabe bei Ablauf der Bearbeitungsfrist automatisch beendet wird. Die Erledigung wurde dabei nicht bestätigt."));

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
    private final AuthoredInputValueService authoredInputValueService;
    public ManualActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
                              ElementDataTransformService elementDataTransformService,
                              ElementDerivationService elementDerivationService,
                              AuthoredInputValueService authoredInputValueService) {
        this.assigneeResolverService = assigneeResolverService;
        this.elementDataTransformService = elementDataTransformService;
        this.elementDerivationService = elementDerivationService;
        this.authoredInputValueService = authoredInputValueService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<ManualActionNodeV1.ManualActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var configuration = context.configurationOfExecutingNode();
        return "Die Erledigung der freien Aufgabe wurde" + summary.eventAt("processedAt") + " durch " + summary.actor() + " bestätigt."
                + section("Aufgabenbeschreibung", configuration.taskDescription)
                + section("Vermerk", summary.data(OUTPUT_REMARK))
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
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
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.Manual};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Freie Aufgabe";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Erstellt eine Mitarbeitendenaufgabe, mit der eine Person die Erledigung eines beschriebenen Arbeitsschritts bestätigt und bei Bedarf Angaben erfasst.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Lassen Sie einen frei beschriebenen Arbeitsschritt erledigen und seine Erledigung im Vorgang bestätigen. Das eignet sich beispielsweise für einen Ortstermin, ein Telefonat oder eine Bearbeitung in einem Fachsystem.

                ### Was richten Sie ein?

                Beschreiben Sie, was erledigt werden soll, und wählen Sie den verantwortlichen Personenkreis aus. Legen Sie bei Bedarf unter „Angaben zur Aufgabe“ eine UI-Definition fest. Sie bestimmt, welche Angaben bei der Bearbeitung angezeigt oder erfasst werden. Über Datenschlüssel verbinden Sie Eingabeelemente mit den zugehörigen Vorgangsdaten. Optional können Sie eine Bearbeitungsfrist einstellen.

                ### Was passiert bei der Ausführung?

                Prosuna weist die Aufgabe einer berechtigten Person aus dem ausgewählten Personenkreis zu. Der Vorgang wartet auf den Abschluss der Aufgabe. Die Bearbeitungsansicht zeigt die Aufgabenbeschreibung und die optional eingerichteten Angaben. Felder mit Datenschlüssel werden mit den entsprechenden Vorgangsdaten vorbelegt.

                Eingaben und ein optionaler Vermerk werden automatisch als Entwurf gespeichert. Mit „Erledigung bestätigen“ bestätigt die bearbeitende Person, dass der beschriebene Arbeitsschritt erledigt wurde. Dabei werden Pflichtangaben und Eingaberegeln geprüft. Anschließend wird der Vorgang über „Erledigung bestätigt“ fortgesetzt.

                ### Welches Ergebnis steht danach bereit?

                Die Bestätigung wird mit Person, Zeitpunkt, Aufgabenbeschreibung und gegebenenfalls Vermerk im Vorgangsverlauf festgehalten. Angaben aus Feldern mit Datenschlüssel werden beim Abschluss in die Vorgangsdaten übernommen. Ohne eingerichtete UI-Definition bleiben die Vorgangsdaten unverändert.

                In den Elementdaten stehen zusätzlich die übernommenen Angaben, der Vermerk, die Kennung der bestätigenden Person, der Zeitpunkt und die Werte nach Element-ID für weitere Schritte bereit. Änderungen an den Vorgangsdaten können Sie über „Datenänderungen“ nachvollziehen.

                ### Wie sieht ein Beispiel aus?

                Die Aufgabenbeschreibung lautet „Führen Sie einen Ortstermin durch und halten Sie das Ergebnis fest.“ Unter „Angaben zur Aufgabe“ richten Sie ein Eingabeelement „Ergebnis des Ortstermins“ mit einem Datenschlüssel ein. Nach dem Termin trägt die bearbeitende Person „Keine Mängel festgestellt“ ein und bestätigt die Erledigung. Das Ergebnis wird in die Vorgangsdaten übernommen; im Verlauf ist erkennbar, wer die Erledigung wann bestätigt hat.

                ### Was ist zu beachten?

                Mit der Bestätigung dokumentiert eine Person die Erledigung des Arbeitsschritts. Die tatsächliche Durchführung einer Arbeit außerhalb von Prosuna wird dabei nicht automatisch geprüft.

                Die Zuweisung legt die zuständige Person fest. Weitere Mitarbeiter:innen mit der Berechtigung zum Bearbeiten von Aufgaben für diesen Vorgang können die Aufgabe ebenfalls bearbeiten und abschließen.

                Ist „Aufgabe bei Fristüberschreitung beenden“ aktiviert, wird die Aufgabe bei Ablauf der Bearbeitungsfrist ohne Erledigungsbestätigung beendet. Der Vorgang wird über „Fristüberschreitung: Bearbeitung“ fortgesetzt. Gespeicherte Entwürfe werden dabei nicht in die Vorgangsdaten übernommen. Ohne diese Option bleibt die Aufgabe nach Fristablauf offen.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(getNodeConfigurationClass());
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e, "Die Einstellungen für die freie Aufgabe konnten nicht geladen werden.", null
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
                    element.setText("Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist die Aufgabe einer berechtigten Person aus diesem Personenkreis zu.");
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
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull ManualActionNodeConfig configuration) {
        return List.of(PROCESSING_DEADLINE);
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull ManualActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_OUTPUT,
                        "Erledigung bestätigt",
                        "Die Erledigung der Aufgabe wurde bestätigt. Der Vorgang wird hier fortgesetzt."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_DATA,
                        "Übernommene Angaben",
                        "Die beim Abschluss übernommenen Werte von Eingabeelementen mit Datenschlüssel, unter ihren Datenschlüsseln zusammengefasst. Enthält auch unveränderte Werte.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_REMARK,
                        "Vermerk",
                        "Der optionale interne Vermerk zur bestätigten Erledigung. Ohne Vermerk ist der Wert null.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_BY_USER_ID,
                        "Bestätigt durch",
                        "Die Kennung des Kontos der Person, die die Erledigung bestätigt hat.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_AT,
                        "Bestätigt am",
                        "Der Zeitpunkt der Bestätigung im ISO-Format.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_UNMAPPED,
                        "Werte nach Element-ID",
                        "Die Werte der Eingabeelemente unter ihrer jeweiligen Element-ID, unabhängig von einem Datenschlüssel.",
                        "Record<string, unknown>"
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
                        "Für die Aufgabe „%s“ konnte im festgelegten Personenkreis keine geeignete Person ermittelt werden. Prüfen Sie den Personenkreis und die erforderlichen Berechtigungen.",
                        context.getThisNode().getName() != null ? context.getThisNode().getName() : getName()
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setNewDeadlineKey(PROCESSING_DEADLINE.key())
                .setProcessData(workingProcessData)
                .setRuntimeData(Map.of());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ManualActionNodeConfig> context) throws ResponseException {
        var config = loadConfigurationForUi(context);
        var layout = buildStaffTaskView(config, context);
        var initialData = config.uiDefinition() != null
                ? authoredInputValueService.toLiteralAuthoredElementValues(
                        config.uiDefinition(),
                        elementDataTransformService.buildEffectiveValues(config.uiDefinition(), context.getThisTask().getProcessData())
                )
                : new AuthoredElementValues();
        return ProcessNodeStaffView.of(
                context,
                layout,
                List.of(new TaskViewEvent("Erledigung bestätigen", EVENT_COMPLETE)),
                initialData
        );
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
            default -> throw unavailableStaffTaskEvent(event, update);
        };
    }

    @Nonnull
    @Override
    public ResponseException unavailableStaffTaskEvent(@Nonnull String event, @Nonnull AuthoredElementValues inputs) {
        return ResponseException.badRequest("Die ausgewählte Aktion ist für diese Aufgabe nicht verfügbar. Aktion: " + event + ".");
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
        descriptionContent.setContent(config.taskDescription());

        var children = new java.util.ArrayList<BaseFormElement>();
        children.add(descriptionHeadline);
        children.add(descriptionContent);

        if (config.uiDefinition() != null) {
            var uiHeadline = new HeadlineContentElement();
            uiHeadline.setId(TASK_VIEW_UI_HEADLINE_ID);
            uiHeadline.setContent("Angaben zur Aufgabe");
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
        remarkField.setHint("Halten Sie bei Bedarf fest, was Sie erledigt haben oder was bei der Bearbeitung zu beachten war.");
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
                    "Hinterlegen Sie eine Aufgabenbeschreibung für die freie Aufgabe."
            );
        }

        var taskDescription = rawTaskDescription.toString();
        if (taskDescription.isBlank()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Hinterlegen Sie eine Aufgabenbeschreibung für die freie Aufgabe."
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
            element = JsonMapperFactory
                    .getInstance()
                    .convertValue(rawUiDefinition, BaseElement.class);
        } catch (JacksonException | IllegalArgumentException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    e,
                    "Die UI-Definition der freien Aufgabe konnte nicht gelesen werden. Prüfen Sie die hinterlegte UI-Definition."
            );
        }

        if (!(element instanceof GroupLayoutElement uiDefinition)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die UI-Definition der freien Aufgabe muss mit einer Gruppe beginnen."
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
                    "Wählen Sie einen verantwortlichen Personenkreis für die freie Aufgabe aus."
            );
        }

        return assignmentContext;
    }

    @Nonnull
    private static GroupLayoutElement cloneDataDefinition(@Nonnull GroupLayoutElement rawElement) {
        var copy = JsonMapperFactory
                .getInstance()
                .convertValue(rawElement, BaseElement.class);

        if (!(copy instanceof GroupLayoutElement copiedElement)) {
            throw new IllegalStateException("Die UI-Definition der freien Aufgabe muss mit einer Gruppe beginnen.");
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
        var updatedProcessData = elementDataTransformService.buildUpdatedProcessData(
                config.uiDefinition(),
                derivedUiUpdate,
                context.getThisTask().getProcessData()
        );
        var remark = normalizeRemark(update.getLiteral(TASK_VIEW_REMARK_FIELD_ID));

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

        @InputElementPOJOBinding(id = TASK_DESCRIPTION_FIELD_ID, type = ElementType.RichTextInput,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Aufgabenbeschreibung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie den Arbeitsschritt, dessen Erledigung mit dem Abschluss dieser Aufgabe bestätigt werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String taskDescription;

        @InputElementPOJOBinding(id = UI_DEFINITION_FIELD_ID, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Angaben zur Aufgabe"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie optional fest, welche Angaben bei der Bearbeitung angezeigt oder erfasst werden sollen. Eingabeelemente mit Datenschlüssel werden mit Vorgangsdaten vorbelegt; ihre Werte werden beim Abschluss in die Vorgangsdaten übernommen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public GroupLayoutElement uiDefinition;

        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "text", strValue = "Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist die Aufgabe einer berechtigten Person aus diesem Personenkreis zu."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public Object assignmentContext;
    }
}
