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
import de.aivot.prosuna.backend.process.exceptions.ManualAssignmentRecipientUnavailableException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.ProcessNodeDeadlineDefinition;
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
import de.aivot.prosuna.backend.user.services.UserDeputyService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.user;

@Component
public class InstanceAssignmentActionNodeV1 implements ProcessNodeDefinition<InstanceAssignmentActionNodeV1.Config> {
    private static final ProcessNodeDeadlineDefinition ASSIGNMENT_DEADLINE = new ProcessNodeDeadlineDefinition(
            "assignment", "Vorgangszuweisung",
            "Zeit für die manuelle Auswahl und Bestätigung der zuständigen Person. Die Frist beginnt mit der Zuweisung der Mitarbeitendenaufgabe.",
            new ProcessNodePort("assignment_overdue", "Fristüberschreitung: Vorgangszuweisung",
                    "Der Vorgang wird hier fortgesetzt, wenn die Zuweisungsaufgabe nach Fristablauf automatisch beendet wurde. Die bisherige Vorgangszuweisung bleibt bestehen."));

    public static final String NODE_KEY = "assign_instance";

    private static final String PORT_SUCCESS = "success";
    private static final String OUTPUT_ASSIGNED_USER_ID = "assignedUserId";
    private static final String MODE_AUTOMATIC = "automatic";
    private static final String MODE_MANUAL = "manual";
    private static final String EVENT_ASSIGN = "assign";
    private static final String TASK_VIEW_ID = "instance-assignment-task-view";
    private static final String TASK_DESCRIPTION_HEADLINE_ID = "instance-assignment-description-headline";
    private static final String TASK_DESCRIPTION_CONTENT_ID = "instance-assignment-description-content";
    private static final String TASK_ASSIGNEE_FIELD_ID = ManualAssignmentRecipientUnavailableException.FIELD_ID;

    private static final List<String> INSTANCE_RECIPIENT_PERMISSIONS = List.of(
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ
    );
    private static final List<String> MANUAL_TASK_PERMISSIONS = List.of(
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_READ,
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_EDIT_TASK,
            ProcessInstancePermissionProvider.PROCESS_INSTANCE_REASSIGN
    );

    private static final List<String> ASSIGNMENT_CONTEXT_TYPES = List.of(
            AssignmentContextInputElement.ALLOWED_TYPE_ORG_UNIT,
            AssignmentContextInputElement.ALLOWED_TYPE_TEAM,
            AssignmentContextInputElement.ALLOWED_TYPE_USER
    );

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ProcessAssignmentService assignmentService;
    private final UserDeputyService deputyService;

    public InstanceAssignmentActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
                                          ProcessAssignmentService assignmentService,
                                          UserDeputyService deputyService) {
        this.assigneeResolverService = assigneeResolverService;
        this.assignmentService = assignmentService;
        this.deputyService = deputyService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<InstanceAssignmentActionNodeV1.Config> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var assignedUserId = summary.data(OUTPUT_ASSIGNED_USER_ID);
        var previousUserId = summary.metadata(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_ID);
        var assignedUser = user(assignedUserId, summary.metadata(ExecutionSummaryMarkdown.ASSIGNED_USER_NAME));
        final String markdown;
        if (previousUserId == null) {
            markdown = "Der Vorgang wurde " + assignedUser + " durch " + summary.actor() + " zugewiesen.";
        } else if (Objects.equals(previousUserId, assignedUserId)) {
            markdown = "Der Vorgang bleibt " + assignedUser + " zugewiesen. Die bestehende Zuweisung wurde durch "
                    + summary.actor() + " beibehalten.";
        } else {
            var previousUser = user(previousUserId, summary.metadata(ExecutionSummaryMarkdown.PREVIOUS_ASSIGNED_USER_NAME));
            markdown = "Die zuständige Person des Vorgangs wurde durch " + summary.actor() + " von " + previousUser
                    + " auf " + assignedUser + " geändert.";
        }
        return markdown + detail("Kurzbeschreibung", context.thisNode().getDescription());
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
        return "Legt die zuständige Person für einen Vorgang automatisch oder durch manuelle Auswahl fest.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Legen Sie mit diesem Prozesselement fest, welche Mitarbeiter:in für einen Vorgang zuständig ist. Prosuna kann die Person automatisch bestimmen oder zunächst eine Mitarbeitendenaufgabe zur manuellen Auswahl erstellen. Der Vorgang wird einer einzelnen Person zugewiesen.

                # Was richten Sie ein?

                Wählen Sie unter „Art der Zuweisung“ zwischen „Automatisch“ und „Manuell“:

                - **Automatisch:** Legen Sie unter „Personenkreis für die Vorgangszuweisung“ Organisationseinheiten, Teams oder einzelne Mitarbeiter:innen fest. Aus diesem Personenkreis ermittelt Prosuna die zuständige Person des Vorgangs. Für diese automatische Auswahl benötigt die Person derzeit eigene Berechtigungen zum Anzeigen des Vorgangs und zum Bearbeiten seiner Aufgaben.
                - **Manuell:** Legen Sie unter „Personenkreis für die Zuweisungsaufgabe“ fest, wer die Auswahl übernehmen soll. Prosuna weist einer berechtigten Person aus diesem Personenkreis eine Mitarbeitendenaufgabe zu. Die zuständige Person des Vorgangs wird anschließend in dieser Aufgabe ausgewählt und kann auch außerhalb des Aufgabenpersonenkreises liegen.

                Unter „Bevorzugung bei der Zuweisung“ können Sie die zuständige Person des vorherigen Prozessschritts oder eine bisher unbeteiligte Person bevorzugen. Kann die Bevorzugung nicht angewendet werden, bleibt die ermittelte Personenauswahl bestehen. Bei mehreren möglichen Personen berücksichtigt Prosuna deren aktuelle Aufgabenlast.

                Für die manuelle Auswahl können Sie eine „Aufgabenbeschreibung“ mit fachlichen Auswahlkriterien hinterlegen und eine Frist einrichten. Die Person, die diese Aufgabe erhält, benötigt die Berechtigungen zum Anzeigen des Vorgangs, zum Bearbeiten der Aufgabe und zum Neuzuweisen des Vorgangs.

                # Was passiert bei der Ausführung?

                Bei automatischer Ausführung legt Prosuna die zuständige Person fest und schließt den Schritt ab.

                Bei manueller Ausführung wartet der Vorgang auf die Zuweisungsaufgabe. Die zugewiesene Person oder eine aktive Stellvertretung wählt unter „Zuständige Person“ einen angebotenen Eintrag aus und bestätigt mit „Vorgang zuweisen“. Die bisher zuständige Person wird nicht automatisch vorausgewählt.

                Für die manuelle Empfängerauswahl sind eine eigene direkte Mitgliedschaft in einer Organisationseinheit oder einem Team, das den Zugang zum Vorgang vermittelt, und eigenes Leserecht darüber erforderlich. Ausschließlich globale Berechtigungen ohne diese Mitgliedschaft und Zugangsquelle oder Rechte aus einer Stellvertretung reichen für diese Auswahl nicht aus.

                Steht während der manuellen Bearbeitung keine Person zur Auswahl, bleibt die Aufgabe offen. Die Auswahl und die Aktion „Vorgang zuweisen“ sind dann nicht verfügbar. Die Berechtigung der ausgewählten Person wird vor der Übernahme erneut geprüft.

                # Welches Ergebnis steht danach bereit?

                Der Vorgang hat eine zuständige Person. Eine vorherige Vorgangszuweisung wird ersetzt. Bestätigen Sie dieselbe Person erneut, bleibt die Zuweisung bestehen und der Schritt wird ebenfalls abgeschlossen. Bereits bestehende Aufgaben behalten ihre eigene Zuweisung.

                Nach erfolgreicher Zuweisung wird der Vorgang über „Zuweisung erfolgt“ fortgesetzt. Die Kennung des Kontos der zuständigen Person steht unter „Zuständige Person“ in den Elementdaten bereit. Über eine Ausgangsdatenzuordnung können Sie diesen Wert bei Bedarf zusätzlich in die Vorgangsdaten übernehmen.

                Die Zusammenfassung im Vorgangsverlauf nennt die zuständige Person und wer die Auswahl vorgenommen hat. Ein Wechsel oder eine unveränderte Zuweisung wird entsprechend beschrieben. Eine hinterlegte Kurzbeschreibung wird ebenfalls angezeigt.

                # Wie sieht ein Beispiel aus?

                Die Teamleitung soll entscheiden, wer einen neuen Vorgang übernimmt. Wählen Sie „Manuell“ und hinterlegen Sie das Team der Teamleitung unter „Personenkreis für die Zuweisungsaufgabe“. Geben Sie in der Aufgabenbeschreibung an, nach welchen fachlichen Kriterien die Auswahl erfolgen soll.

                Eine berechtigte Person aus diesem Team erhält die Aufgabe. Sie wählt beispielsweise „Mara Beispiel“ aus der aktuellen Empfängerliste und bestätigt mit „Vorgang zuweisen“. Danach ist Mara Beispiel die zuständige Person des Vorgangs. Die auswählbaren Personen müssen nicht zum Team gehören, das die Zuweisungsaufgabe bearbeitet.

                # Was ist zu beachten?

                Die Personenkreise und die dort gewählten Bevorzugungen bestimmen die automatische Auswahl der zuständigen Person beziehungsweise der Person für die Zuweisungsaufgabe. Eine Bevorzugung garantiert keine bestimmte Auswahl und ist kein fester Ausschluss anderer Personen.

                Kann bei Beginn des Elements keine berechtigte Person aus dem jeweiligen Personenkreis ermittelt werden, schlägt die Aufgabe fehl. Die Zuweisung selbst ersetzt keine erforderlichen Berechtigungen.

                Die Frist für die manuelle Auswahl beginnt mit der Zuweisung der Mitarbeitendenaufgabe. Ist „Aufgabe bei Fristüberschreitung beenden“ aktiviert, beendet Prosuna die offene Aufgabe nach Fristablauf und wählt „Fristüberschreitung: Vorgangszuweisung“. Die bisherige Vorgangszuweisung bleibt dabei bestehen. Ohne automatische Beendigung bleibt die Aufgabe auch nach Fristablauf bearbeitbar.

                Bei einer Änderung der Zuweisung versucht Prosuna, die neu und gegebenenfalls bisher zuständige Person per E-Mail zu informieren. Versandfehler werden im technischen Protokoll festgehalten. Bleibt dieselbe Person zuständig, wird keine erneute Benachrichtigung versendet.
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
            throw ResponseException.internalServerError(e, "Die Einstellungen für „Vorgang zuweisen“ konnten nicht geladen werden.");
        }

        layout.findChild(Config.MODE_FIELD_ID, SelectInputElement.class)
                .ifPresent(field -> field.setOptions(List.of(
                        SelectInputElementOption.of(MODE_AUTOMATIC, "Automatisch"),
                        SelectInputElementOption.of(MODE_MANUAL, "Manuell")
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
                    field.setAllowedTypes(ASSIGNMENT_CONTEXT_TYPES);
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
                    field.setAllowedTypes(ASSIGNMENT_CONTEXT_TYPES);
                    field.setDisableProcessInstanceAssigneeOption(true);
                    field.setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);

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
            return Map.of(Config.MODE_FIELD_ID, List.of("Wählen Sie unter „Art der Zuweisung“ entweder „Automatisch“ oder „Manuell“."));
        }

        var fieldId = MODE_AUTOMATIC.equals(mode)
                ? Config.AUTOMATIC_CONTEXT_FIELD_ID : Config.MANUAL_CONTEXT_FIELD_ID;
        var rawContext = MODE_AUTOMATIC.equals(mode)
                ? config.automaticAssignmentContext : config.manualAssignmentContext;
        var assignmentContext = AssignmentContextInputElement._formatValue(rawContext);
        if (assignmentContext == null || assignmentContext.getDomainAndUserSelection() == null
                || assignmentContext.getDomainAndUserSelection().isEmpty()) {
            return Map.of(fieldId, List.of("Wählen Sie einen Personenkreis für die ausgewählte Art der Zuweisung aus."));
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
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull Config configuration) {
        return MODE_MANUAL.equals(normalizeMode(configuration.mode))
                ? List.of(ASSIGNMENT_DEADLINE) : List.of();
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull Config configuration) {
        return List.of(new ProcessNodePort(
                PORT_SUCCESS,
                "Zuweisung erfolgt",
                "Der Vorgang wird hier fortgesetzt, nachdem die zuständige Person des Vorgangs festgelegt wurde."
        ));
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(new ProcessNodeOutput(OUTPUT_ASSIGNED_USER_ID, "Zuständige Person",
                "Die Kennung des Kontos der Person, der der Vorgang nach erfolgreicher Zuweisung zugewiesen ist.", "string"));
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
                    "Die für die Vorgangszuweisung verfügbaren Personen konnten nicht ermittelt werden.");
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
                                ? "Für die automatische Vorgangszuweisung konnte keine berechtigte Person aus dem festgelegten Personenkreis ermittelt werden."
                                : "Für die Aufgabe zur Auswahl der zuständigen Person konnte keine berechtigte Person aus dem festgelegten Personenkreis ermittelt werden."
                ));

        if (MODE_MANUAL.equals(mode)) {
            return ProcessNodeExecutionResultTaskAssigned.of(selectedUserId)
                    .setNewDeadlineKey(ASSIGNMENT_DEADLINE.key())
                    .setProcessData(context.getCurrentProcessExecutionData().getProcessData())
                    .setRuntimeData(Map.of());
        }

        return assignmentResult(selectedUserId, context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context) throws ResponseException {
        if (!MODE_MANUAL.equals(normalizeMode(context.getConfigurationOfExecutingNode().mode))) {
            throw ResponseException.badRequest("Für diese Vorgangszuweisung ist keine manuelle Bearbeitung vorgesehen.");
        }
        requireAssignedTaskUser(context);

        var options = assignmentService.manualInstanceOptions(context.getCallingUser().getId(),
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

        var assigneeField = new SelectInputElement() {
            @Override
            public void performValidation(@Nullable String value) throws ValidationException {
                if (value != null && !containsOptionValue(value)) {
                    var unavailable = new ManualAssignmentRecipientUnavailableException(value);
                    throw new ValidationException(this, unavailable.getMessage(), unavailable.getDetails());
                }
                super.performValidation(value);
            }
        };
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
    public ResponseException unavailableStaffTaskEvent(@Nonnull String event, @Nonnull AuthoredElementValues inputs) {
        // The assignment action disappears from the regenerated view when its last option is withdrawn.
        var selected = StringUtils.toNullableTrimmedString(inputs.getLiteral(TASK_ASSIGNEE_FIELD_ID));
        if (EVENT_ASSIGN.equals(event) && selected != null) {
            return new ManualAssignmentRecipientUnavailableException(selected);
        }
        return ProcessNodeDefinition.super.unavailableStaffTaskEvent(event, inputs);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onAutoSaveFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context,
                                                                          @Nonnull AuthoredElementValues update)
            throws ResponseException, ProcessNodeExecutionException {
        requireAssignedTaskUser(context);
        var selected = StringUtils.toNullableTrimmedString(update.getLiteral(TASK_ASSIGNEE_FIELD_ID));
        if (selected != null) {
            try {
                assignmentService.requireManualInstanceAssignee(context.getCallingUser().getId(),
                        context.getThisProcessInstance().getId(), selected);
            } catch (ManualAssignmentRecipientUnavailableException ignored) {
                update = update.clone();
                update.remove(TASK_ASSIGNEE_FIELD_ID);
            }
        }
        return ProcessNodeDefinition.super.onAutoSaveFromStaffTaskView(context, update);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException {
        if (!EVENT_ASSIGN.equals(event)
                || !MODE_MANUAL.equals(normalizeMode(context.getConfigurationOfExecutingNode().mode))) {
            throw ResponseException.badRequest("Die ausgewählte Aktion ist für diese Vorgangszuweisung nicht verfügbar.");
        }
        requireAssignedTaskUser(context);

        var selectedUserId = StringUtils.toNullableTrimmedString(update.getLiteral(TASK_ASSIGNEE_FIELD_ID));
        if (selectedUserId == null) {
            throw ResponseException.badRequest("Wählen Sie eine zuständige Person aus.");
        }

        assignmentService.requireManualInstanceAssignee(context.getCallingUser().getId(),
                context.getThisProcessInstance().getId(), selectedUserId);

        return Optional.of(assignmentResult(selectedUserId, context.getThisTask().getProcessData())
                .requireDirectMembership());
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
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie unter „Art der Zuweisung“ entweder „Automatisch“ oder „Manuell“.");
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
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie einen Personenkreis für die ausgewählte Art der Zuweisung aus.");
        }
        try {
            restrictedAssignmentContextElement().performValidation(value);
        } catch (ValidationException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e.getMessage());
        }
        return value;
    }

    // Must allow the same entries as both context fields in getConfigurationLayout.
    @Nonnull
    private static AssignmentContextInputElement restrictedAssignmentContextElement() {
        return new AssignmentContextInputElement()
                .setAllowedTypes(ASSIGNMENT_CONTEXT_TYPES)
                .setDisableProcessInstanceAssigneeOption(true)
                .setDisableAssignmentContextRepeatExecutionAssigneePreferenceOptions(true);
    }

    private void requireAssignedTaskUser(@Nonnull ProcessNodeExecutionContextUIStaff<Config> context)
            throws ResponseException {
        var callingUserId = context.getCallingUser().getId();
        var assignedUserId = context.getThisTask().getAssignedUserId();
        // Only the assignee decides the assignment, other staff do not even with instance rights. An active deputy
        // takes over the assignee's tasks, so the process does not stall during an absence.
        if (assignedUserId == null || !(callingUserId.equals(assignedUserId)
                || deputyService.isActiveDeputyFor(callingUserId, assignedUserId))) {
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
        @InputElementPOJOBinding(id = MODE_FIELD_ID, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "presentation", strValue = "radio"),
                @ElementPOJOBindingProperty(key = "label", strValue = "Art der Zuweisung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, ob Prosuna die zuständige Person automatisch ermittelt oder ob sie in einer Mitarbeitendenaufgabe ausgewählt wird."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String mode;

        /**
         * Candidate circle used only in automatic mode. An empty circle prevents execution.
         */
        @Nullable
        @InputElementPOJOBinding(id = AUTOMATIC_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Personenkreis für die Vorgangszuweisung"),
                @ElementPOJOBindingProperty(key = "text", strValue = "Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist den Vorgang einer berechtigten Person aus diesem Personenkreis zu."),
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
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie, nach welchen Kriterien die zuständige Person des Vorgangs ausgewählt werden soll."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String taskDescription;

        /**
         * Candidate circle for assigning the manual task; recipients are selected separately in the task view.
         */
        @Nullable
        @InputElementPOJOBinding(id = MANUAL_CONTEXT_FIELD_ID, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Personenkreis für die Zuweisungsaufgabe"),
                @ElementPOJOBindingProperty(key = "text", strValue = "Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist die Aufgabe einer berechtigten Person aus diesem Personenkreis zu."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue manualAssignmentContext;
    }
}
