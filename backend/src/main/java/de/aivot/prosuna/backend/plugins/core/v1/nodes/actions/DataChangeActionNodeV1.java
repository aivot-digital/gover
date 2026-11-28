package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
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

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;

@Component
public class DataChangeActionNodeV1 implements ProcessNodeDefinition<DataChangeActionNodeV1.DataChangeActionNodeConfig> {
    private static final ProcessNodeDeadlineDefinition DATA_ENTRY_DEADLINE = new ProcessNodeDeadlineDefinition(
            "dataEntry", "Bearbeitung der Vorgangsdaten",
            "Zeit für die Bearbeitung der Vorgangsdaten ab der Zuweisung der Aufgabe.",
            new ProcessNodePort("data_entry_overdue", "Bearbeitungsfrist überschritten",
                    "Der Vorgang wird hier fortgesetzt, wenn die Aufgabe nach Ablauf der Bearbeitungsfrist automatisch beendet wurde. Ein gespeicherter Entwurf wird dabei nicht in die Vorgangsdaten übernommen."));

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
    private final AuthoredInputValueService authoredInputValueService;

    public DataChangeActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
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
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<DataChangeActionNodeV1.DataChangeActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        return "Die Bearbeitung der Vorgangsdaten wurde" + summary.eventAt("processedAt") + " durch " + summary.actor() + " abgeschlossen."
                + section("Änderungsvermerk", summary.data(OUTPUT_REMARK));
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
        return "Vorgangsdaten bearbeiten";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Erstellt eine Aufgabe, in der Mitarbeiter:innen ausgewählte Vorgangsdaten ergänzen oder ändern.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Mit diesem Prozesselement lassen Sie Mitarbeiter:innen ausgewählte Vorgangsdaten ergänzen oder ändern, zum Beispiel eine Anschrift berichtigen oder interne Angaben hinzufügen.

                Sie legen fest, welche Felder für die Bearbeitung bereitstehen. Eine fachliche Entscheidung mit Freigabe und Ablehnung ist damit nicht verbunden.

                # Was richten Sie ein?

                Gestalten Sie unter **„Ansicht für die Datenbearbeitung“** die benötigten Felder und Hinweise mit einer UI-Definition. Über den Datenschlüssel eines Feldes legen Sie fest, welche Vorgangsdaten angezeigt und beim Abschluss übernommen werden. Felder ohne Datenschlüssel ändern die Vorgangsdaten nicht direkt.

                Wählen Sie den **„Verantwortlichen Personenkreis“** aus. Sie können außerdem Bevorzugungen für die Zuweisung festlegen, auch für eine erneute Ausführung in einer Schleife.

                Bei Bedarf richten Sie eine Frist für die **„Bearbeitung der Vorgangsdaten“** ein. Entscheiden Sie dabei, ob die Aufgabe bei Fristüberschreitung automatisch beendet werden soll. Verbinden Sie in diesem Fall auch den Ausgang **„Bearbeitungsfrist überschritten“** mit dem nächsten Schritt.

                # Was passiert bei der Ausführung?

                Prosuna weist die Aufgabe einer berechtigten Person aus dem festgelegten Personenkreis zu. Die Oberfläche wird anhand der Datenschlüssel mit den vorhandenen Vorgangsdaten vorbelegt. Ein gespeicherter Zwischenstand wird beim erneuten Öffnen wieder angezeigt.

                Die zuständige Person bearbeitet die Angaben und kann einen **„Änderungsvermerk“** erfassen. Unvollständige Zwischenstände werden automatisch gespeichert. Beim Abschluss müssen die Pflichtangaben und Feldprüfungen erfüllt sein.

                Mit **„Daten übernehmen“** schließt die Person die Aufgabe ab. Der Vorgang wird über **„Daten übernommen“** fortgesetzt. Ein Abschluss ist auch möglich, wenn keine Werte geändert wurden.

                # Welches Ergebnis steht danach bereit?

                Die Angaben aus Feldern mit Datenschlüssel werden in die Vorgangsdaten übernommen. Andere Vorgangsdaten bleiben erhalten. Hochgeladene Dateien werden als Anlagen des Vorgangs gespeichert.

                In den Elementdaten stehen die übernommenen Angaben, die ausgewerteten Feldwerte nach Element-ID, der optionale Änderungsvermerk sowie die bearbeitende Person und der Abschlusszeitpunkt bereit. Über die Datenzuordnung können Sie weitere Ergebnisse, etwa den Vermerk, zusätzlich in die Vorgangsdaten übernehmen.

                Die Zusammenfassung im Vorgangsverlauf zeigt, wer die Bearbeitung abgeschlossen hat und wann. Ein erfasster Änderungsvermerk wird ebenfalls angezeigt. Unter **„Datenänderungen“** können Sie die gespeicherten Datenstände vergleichen, sofern der vorherige Stand verfügbar ist.

                # Wie sieht ein Beispiel aus?

                Eine zuständige Person soll die Anschrift in einem Vorgang berichtigen. Legen Sie dafür ein Textfeld mit dem Datenschlüssel `person.anschrift` an und wählen Sie den verantwortlichen Personenkreis aus.

                Das Feld zeigt die vorhandene Anschrift. Die Person trägt die neue Anschrift ein und erläutert im Änderungsvermerk zum Beispiel „Anschrift anhand der Rückmeldung berichtigt“. Mit **„Daten übernehmen“** wird die Anschrift in den Vorgangsdaten aktualisiert. Andere Angaben zur Person bleiben erhalten.

                # Was ist zu beachten?

                Ein gespeicherter Zwischenstand ist noch keine Übernahme in die Vorgangsdaten. Hochgeladene Anlagen können jedoch bereits während der Bearbeitung gespeichert werden.

                Ein Feld zu leeren entfernt nicht automatisch seinen Datenschlüssel aus den Vorgangsdaten. Ob ein leerer Wert zulässig ist, bestimmen die Feldregeln.

                Eine Bevorzugung bei der Zuweisung garantiert keine bestimmte Person und keinen verbindlichen Ausschluss einer zuvor beteiligten Person.

                Ohne automatische Beendigung bleibt die Aufgabe nach Fristüberschreitung bearbeitbar. Mit automatischer Beendigung wird die Bearbeitung ohne Übernahme des Entwurfs beendet. Dieser Fristablauf ist kein regulärer Abschluss der Datenbearbeitung.
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
                    e, "Die Einstellungen für die Datenbearbeitung konnten nicht geladen werden.", null
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
        configuration.remove(DataChangeActionNodeConfig.ASSIGNMENT_CONTEXT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    @Override
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull DataChangeActionNodeConfig configuration) {
        return List.of(DATA_ENTRY_DEADLINE);
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull DataChangeActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_OUTPUT,
                        "Daten übernommen",
                        "Der Vorgang wird hier fortgesetzt, nachdem die Bearbeitung abgeschlossen und die Angaben übernommen wurden."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_DATA,
                        "Übernommene Daten",
                        "Die beim Abschluss übernommenen Angaben aus Feldern mit Datenschlüssel. Enthält auch unverändert übernommene Werte und ist kein Änderungsvergleich.",
                        "Record<string, unknown>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_REMARK,
                        "Änderungsvermerk",
                        "Der beim Abschluss freiwillig erfasste Vermerk zur Bearbeitung.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_BY_USER_ID,
                        "Bearbeitet durch",
                        "Die Kennung des Kontos der Person, die die Bearbeitung abgeschlossen hat.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_AT,
                        "Abgeschlossen am",
                        "Der Zeitpunkt des Abschlusses im technischen ISO-Zeitformat.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_UNMAPPED,
                        "Feldwerte nach Element-ID",
                        "Die ausgewerteten Werte der Felder unter ihrer jeweiligen Element-ID, auch ohne Datenschlüssel.",
                        "Record<string, unknown>"
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
                        "Für das Prozesselement „%s“ konnte keine berechtigte Person aus dem festgelegten Personenkreis für die Bearbeitung ermittelt werden.",
                        context.getThisNode().getName() != null ? context.getThisNode().getName() : getName()
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setNewDeadlineKey(DATA_ENTRY_DEADLINE.key())
                .setProcessData(workingProcessData)
                .setRuntimeData(Map.of());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<DataChangeActionNodeConfig> context) throws ResponseException {
        var config = context.getConfigurationOfExecutingNode();

        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var children = new java.util.ArrayList<BaseFormElement>();

        var uiHeadline = new HeadlineContentElement();
        uiHeadline.setId(TASK_VIEW_UI_HEADLINE_ID);
        uiHeadline.setContent("Vorgangsdaten bearbeiten");
        children.add(uiHeadline);
        children.add(cloneDataDefinition(config.dataDefinition));

        var remarkSpacer = new SpacerContentElement();
        remarkSpacer.setId(TASK_VIEW_REMARK_SPACER_ID);
        remarkSpacer.setHeight("16");
        children.add(remarkSpacer);

        var remarkField = new RichTextInputElement();
        remarkField.setId(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setLabel("Änderungsvermerk");
        remarkField.setHint("Erläutern Sie bei Bedarf Ihre Bearbeitung. Der Vermerk erscheint in der Zusammenfassung der Aufgabe.");
        remarkField.setRequired(false);
        remarkField.setWeight(6.0);
        children.add(remarkField);

        layout.setChildren(children);
        var effectiveValues = elementDataTransformService
                .buildEffectiveValues(config.dataDefinition, context.getThisTask().getProcessData());
        var initialData = authoredInputValueService.toLiteralAuthoredElementValues(config.dataDefinition, effectiveValues);
        return ProcessNodeStaffView.of(
                context,
                layout,
                List.of(new TaskViewEvent("Daten übernehmen", EVENT_COMPLETE)),
                initialData
        );
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
            default -> throw ResponseException.badRequest("Die ausgewählte Aktion ist für diese Aufgabe nicht verfügbar. Aktion: " + event + ".");
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
        var updatedProcessData = elementDataTransformService.buildUpdatedProcessData(
                config.dataDefinition,
                derivedRuntimeData,
                context.getThisTask().getProcessData()
        );
        var remark = normalizeRemark(authoredUpdate.getLiteral(TASK_VIEW_REMARK_FIELD_ID));

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
        var copy = JsonMapperFactory
                .getInstance()
                .convertValue(rawElement, BaseElement.class);

        if (!(copy instanceof GroupLayoutElement copiedElement)) {
            throw new IllegalStateException("Die Ansicht für die Datenbearbeitung konnte nicht geladen werden. Die UI-Definition muss eine Gruppe von Formularelementen enthalten.");
        }

        return copiedElement;
    }

    @Nonnull
    private static Map<String, Object> extractWorkingProcessData(@Nonnull Map<String, Object> processData)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var rawWorkingProcessData = processData.get("$");

        if (!(rawWorkingProcessData instanceof Map<?, ?> rawWorkingProcessDataMap)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Vorgangsdaten haben ein ungültiges Format. Für die Datenbearbeitung wird am Wurzelpfad ($) ein Datenobjekt erwartet."
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
                @ElementPOJOBindingProperty(key = "label", strValue = "Ansicht für die Datenbearbeitung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Gestalten Sie mit einer UI-Definition die Felder und Hinweise für die Bearbeitung. Über den Datenschlüssel eines Feldes legen Sie fest, welche Vorgangsdaten angezeigt und beim Abschluss übernommen werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public GroupLayoutElement dataDefinition;

        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "text", strValue = "Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist die Aufgabe einer berechtigten Person aus diesem Personenkreis zu."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }
}
