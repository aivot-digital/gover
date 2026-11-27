package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.ElementDisplayContext;
import de.aivot.prosuna.backend.elements.enums.InputMode;
import de.aivot.prosuna.backend.elements.enums.ValueFunctionType;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.elements.BaseFormElement;
import de.aivot.prosuna.backend.elements.models.elements.ElementValueFunctions;
import de.aivot.prosuna.backend.elements.models.elements.ElementVisibilityFunctions;
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
import de.aivot.prosuna.backend.nocode.models.NoCodeExpression;
import de.aivot.prosuna.backend.nocode.models.NoCodeReference;
import de.aivot.prosuna.backend.nocode.models.NoCodeStaticValue;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.plugins.core.v1.operators.common.NoCodeEqualsOperator;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
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
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.process.utils.ProcessHistoryLabels;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;

@Component
public class ApprovalActionNodeV1 implements ProcessNodeDefinition<ApprovalActionNodeV1.ApprovalConfiguration> {
    private static final ProcessNodeDeadlineDefinition APPROVAL_DEADLINE = new ProcessNodeDeadlineDefinition(
            "approval", "Freigabeentscheidung",
            "Zeit ab Zuweisung der Aufgabe, innerhalb derer die Freigabeentscheidung getroffen werden soll.",
            new ProcessNodePort("approval_overdue", "Freigabefrist überschritten",
                    "Der Vorgang wird hier fortgesetzt, wenn die Frist überschritten und die Freigabeaufgabe automatisch beendet wurde."));

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
    private static final String OUTPUT_UNMAPPED = "unmapped";

    private final AssignmentContextAssigneeResolverService assigneeResolverService;
    private final ElementDataTransformService elementDataTransformService;
    private final ElementDerivationService elementDerivationService;
    private final AuthoredInputValueService authoredInputValueService;

    public ApprovalActionNodeV1(AssignmentContextAssigneeResolverService assigneeResolverService,
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
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<ApprovalActionNodeV1.ApprovalConfiguration> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var configuration = context.configurationOfExecutingNode();
        var decision = PORT_APPROVED.equals(context.viaPort()) ? "erteilt" : "abgelehnt";
        return "Die Freigabe wurde" + summary.eventAt("processedAt") + " durch " + summary.actor() + " " + decision + "."
                + section("Vermerk", summary.data(OUTPUT_REMARK))
                + section("Freigabekriterien", configuration.criteria)
                + section("Geprüfte Inhalte", MODE_CUSTOM_CONTENT.equals(configuration.contentMode)
                        ? configuration.customContent : null);
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
        return "Freigabe einholen";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Lässt eine zuständige Person Inhalte prüfen und die Freigabe erteilen oder ablehnen.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                # Wofür verwenden Sie dieses Prozesselement?

                Holen Sie eine fachliche Entscheidung ein, bevor ein Vorgang weiterbearbeitet wird. Die zuständige Person prüft die bereitgestellten Inhalte anhand Ihrer Freigabekriterien und erteilt die Freigabe oder lehnt sie ab.

                Über die beiden Ausgänge legen Sie fest, wie der Vorgang nach einer Freigabe oder Ablehnung weitergeht.

                # Was richten Sie ein?

                Beschreiben Sie unter **„Freigabekriterien“**, was geprüft werden soll und unter welchen Voraussetzungen eine Freigabe erteilt werden darf.

                Wählen Sie unter **„Zu prüfende Inhalte“** eine Option:

                - **Datenansicht:** Gestalten Sie mit einer UI-Definition eine Oberfläche mit Feldern und Hinweisen. Sie kann vorhandene Vorgangsdaten anzeigen und Angaben erfassen oder ändern.
                - **Eigener Text:** Beschreiben Sie den Prüfgegenstand als Text, beispielsweise wenn die Prüfung in einem Fachsystem stattfindet.

                Legen Sie unter **„Verantwortlicher Personenkreis“** fest, welche Organisationseinheiten, Teams oder Mitarbeiter:innen für die Aufgabe infrage kommen. Optional stellen Sie Bevorzugungen für die Zuweisung ein.

                Unter den weiteren Eigenschaften können Sie eine Frist in Tagen festlegen. Entscheiden Sie dort auch, ob die Aufgabe bei einer Fristüberschreitung automatisch beendet werden soll.

                # Was passiert bei der Ausführung?

                Prosuna erstellt eine Mitarbeitendenaufgabe und weist sie einer berechtigten Person aus dem gewählten Personenkreis zu. Diese Person sieht die Freigabekriterien und die zu prüfenden Inhalte. Sie kann einen Vermerk ergänzen und **„Freigabe erteilen“** oder **„Freigabe ablehnen“** wählen.

                Bis zur Entscheidung wartet dieser Schritt. Danach wird die Aufgabe abgeschlossen und der Vorgang über **„Freigabe erteilt“** beziehungsweise **„Freigabe abgelehnt“** fortgesetzt.

                Haben Sie eine automatische Beendigung bei Fristüberschreitung eingerichtet, kann die Aufgabe stattdessen ohne fachliche Entscheidung enden. Der Vorgang folgt dann dem Ausgang **„Freigabefrist überschritten“**.

                # Welches Ergebnis steht danach bereit?

                Nach einer Entscheidung stehen das Ergebnis, ein gegebenenfalls erfasster Vermerk sowie Angaben zur entscheidenden Person und zum Zeitpunkt in den Elementdaten bereit. Über die Datenzuordnung können Sie diese Ergebnisse zusätzlich in die Vorgangsdaten übernehmen.

                Bei einer Datenansicht werden die Angaben aus Feldern mit Datenschlüssel in die Vorgangsdaten übernommen. Das gilt sowohl bei einer Freigabe als auch bei einer Ablehnung.

                Der Vorgangsverlauf hält fest, wer die Entscheidung getroffen hat und wie sie ausgefallen ist. Die Zusammenfassung enthält außerdem den Vermerk, die Freigabekriterien und bei einem eigenen Text die beschriebenen Prüfinhalte.

                # Wie sieht ein Beispiel aus?

                Vor dem Versand eines Schreibens soll eine zuständige Person prüfen, ob die fachlichen Voraussetzungen erfüllt sind.

                Beschreiben Sie diese Voraussetzungen als Freigabekriterien und wählen Sie einen Personenkreis aus. Stellen Sie die benötigten Angaben über eine Datenansicht bereit.

                Verbinden Sie **„Freigabe erteilt“** mit dem Versand des Schreibens und **„Freigabe abgelehnt“** mit einer Aufgabe zur Überarbeitung. Die prüfende Person kann im Vermerk erläutern, welche Voraussetzungen noch fehlen.

                # Was ist zu beachten?

                Die Entscheidung trifft eine Person. Die Freigabekriterien werden nicht automatisch fachlich geprüft.

                Eine Ablehnung nimmt Änderungen in der Datenansicht nicht zurück. Angaben aus Feldern mit Datenschlüssel werden bei beiden Entscheidungen übernommen. Modellieren Sie Felder entsprechend, wenn sie nur angezeigt oder bearbeitet werden sollen. Erforderliche Angaben und Feldprüfungen gelten auch beim Ablehnen.

                Eine Bevorzugung bei der Zuweisung garantiert keine bestimmte Person und keine verbindliche Trennung zwischen vorheriger Bearbeitung und Freigabe.

                Ohne automatische Beendigung bleibt eine Aufgabe nach Fristüberschreitung bearbeitbar; die Überschreitung wird angezeigt. Mit automatischer Beendigung ist anschließend keine Freigabeentscheidung mehr möglich. Dieser Fristablauf gilt weder als Freigabe noch als Ablehnung.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(ApprovalConfiguration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Die Konfiguration des Prozesselements konnte nicht geladen werden.");
        }

        // Configure the content mode field
        layout
                .findChild(CONTENT_MODE_FIELD_ID, SelectInputElement.class)
                .ifPresent(contentModeField -> {
                    contentModeField.setValue(new ElementValueFunctions()
                            .setType(ValueFunctionType.NoCode)
                            .setNoCode(new NoCodeStaticValue(MODE_DATA)));
                    contentModeField.setOptions(List.of(
                            SelectInputElementOption.of(MODE_DATA, "Datenansicht"),
                            SelectInputElementOption.of(MODE_CUSTOM_CONTENT, "Eigener Text")
                    ));
                });

        // Configure the ui definition modeling field
        layout
                .findChild(DATA_CONTENT_FIELD_ID, UiDefinitionInputElement.class)
                .ifPresent(dataContentField -> {
                    dataContentField.setElementType(ElementType.GroupLayout);
                    dataContentField.setVisibility(buildModeVisibility(MODE_DATA));
                    dataContentField.setDisplayContext(ElementDisplayContext.StaffFacing);
                });

        // Configure the text content field
        layout
                .findChild(CUSTOM_CONTENT_FIELD_ID, RichTextInputElement.class)
                .ifPresent(customContentField -> {
                    customContentField.setVisibility(buildModeVisibility(MODE_CUSTOM_CONTENT));
                });

        // Configure the assignment field
        layout
                .findChild(ASSIGNMENT_CONTEXT_FIELD_ID, AssignmentContextInputElement.class)
                .ifPresent(assignmentContextField -> {
                    assignmentContextField.setAllowedTypes(List.of("orgUnit", "team", "user"));
                    assignmentContextField.setProcessAccessConstraint(new DomainAndUserSelectProcessAccessConstraint()
                            .setProcessId(context.processDefinition().getId())
                            .setProcessVersion(context.processDefinitionVersion().getProcessVersion())
                            .setRequiredPermissions(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)));
                });

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull ApprovalConfiguration configuration) {
        return List.of(APPROVAL_DEADLINE);
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull ApprovalConfiguration configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_APPROVED,
                        "Freigabe erteilt",
                        "Der Vorgang wird hier fortgesetzt, nachdem die zuständige Person die Freigabe erteilt hat."
                ),
                new ProcessNodePort(
                        PORT_REJECTED,
                        "Freigabe abgelehnt",
                        "Der Vorgang wird hier fortgesetzt, nachdem die zuständige Person die Freigabe abgelehnt hat."
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
                        "Das Ergebnis der Entscheidung: Freigabe erteilt („approved“) oder Freigabe abgelehnt („rejected“).",
                        "\"approved\" | \"rejected\""
                ),
                new ProcessNodeOutput(
                        OUTPUT_REMARK,
                        "Vermerk",
                        "Der bei der Freigabe oder Ablehnung erfasste Vermerk.",
                        "string | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_BY_USER_ID,
                        "Entschieden durch",
                        "Die Kennung des Kontos der Person, die die Entscheidung getroffen hat.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PROCESSED_AT,
                        "Entschieden am",
                        "Der Zeitpunkt, zu dem die Entscheidung getroffen wurde, im technischen ISO-Zeitformat.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_UNMAPPED,
                        "Feldwerte nach Element-ID",
                        "Die Werte aus der Aufgabenansicht unter ihrer jeweiligen Element-ID. Dazu gehört auch der Vermerk. Ein Datenschlüssel ist dafür nicht erforderlich.",
                        "Record<string, unknown>"
                )
        );
    }

    @Nonnull
    @Override
    public ProcessNodeDefinitionMetadata getMetadata(@Nonnull ProcessNodeEntity processNodeEntity,
                                                     @Nonnull ApprovalConfiguration configuration,
                                                     @Nonnull ProcessNodeDefinitionMetadata previousMetadata) {
        if (!Objects.equals(configuration.contentMode, MODE_CUSTOM_CONTENT)) {
            return ProcessNodeDefinitionMetadata
                    .reuse(previousMetadata)
                    .withLayout(configuration.dataContent, processNodeEntity);
        }
        return previousMetadata;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<ApprovalConfiguration> context) throws ProcessNodeExecutionException {
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
                        "Für %s konnte keine zuständige Person ermittelt werden. Prüfen Sie den Personenkreis und die Berechtigungen zum Bearbeiten der Aufgabe.",
                        StringUtils.quote(this.resolveNodeName(context.getThisNode()))
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setNewDeadlineKey(APPROVAL_DEADLINE.key())
                .setProcessData(workingProcessData)
                .setRuntimeData(Map.of());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context) throws ResponseException {
        var config = context.getConfigurationOfExecutingNode();

        var layout = new GroupLayoutElement();
        layout.setId(TASK_VIEW_ROOT_ID);

        var children = new ArrayList<BaseFormElement>();
        var criteriaHeadline = new HeadlineContentElement();
        criteriaHeadline.setId("approval-criteria-headline");
        criteriaHeadline.setContent("Freigabekriterien");
        children.add(criteriaHeadline);

        var criteriaContent = new RichTextContentElement();
        criteriaContent.setId("approval-criteria-content");
        criteriaContent.setContent(config.criteria);
        children.add(criteriaContent);

        var contentHeadline = new HeadlineContentElement();
        contentHeadline.setId("approval-content-headline");
        contentHeadline.setContent("Zu prüfende Inhalte");
        children.add(contentHeadline);

        if (MODE_DATA.equals(config.contentMode)) {
            children.add(config.dataContent);
        } else {
            var customContent = new RichTextContentElement();
            customContent.setId("approval-custom-content");
            customContent.setContent(config.customContent);
            children.add(customContent);
        }

        var remarkField = new RichTextInputElement();
        remarkField.setId(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setDestinationKey(TASK_VIEW_REMARK_FIELD_ID);
        remarkField.setLabel("Vermerk");
        remarkField.setHint("Optional. Erläutern Sie Ihre Entscheidung oder halten Sie Hinweise für die weitere Bearbeitung fest.");
        remarkField.setRequired(false);
        remarkField.setWeight(6.0);
        children.add(remarkField);

        var actionsSpacer = new SpacerContentElement();
        actionsSpacer.setId(TASK_VIEW_ACTIONS_SPACER_ID);
        actionsSpacer.setHeight("8");
        children.add(actionsSpacer);

        layout.setChildren(children);

        var events = List.of(
                new TaskViewEvent("Freigabe erteilen", EVENT_APPROVE),
                new TaskViewEvent("Freigabe ablehnen", EVENT_REJECT)
        );
        var effectiveValues = elementDataTransformService.buildEffectiveValues(
                layout, context.getThisTask().getProcessData()
        );
        var initialData = authoredInputValueService.toLiteralAuthoredElementValues(layout, effectiveValues);
        return ProcessNodeStaffView.of(context, layout, events, initialData);
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<ApprovalConfiguration> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException {
        var remark = update.getLiteral(TASK_VIEW_REMARK_FIELD_ID);
        var remarkText = remark != null ? remark.toString() : null;

        final String port;
        final String decision;
        final boolean approved;
        if (EVENT_APPROVE.equals(event)) {
            port = PORT_APPROVED;
            decision = PORT_APPROVED;
            approved = true;
        } else if (EVENT_REJECT.equals(event)) {
            port = PORT_REJECTED;
            decision = PORT_REJECTED;
            approved = false;
        } else {
            throw ResponseException.badRequest("Unbekannte Aktion: " + event);
        }

        // Derive the effective values based on the staff task view and the saved staff task view data to store the unmapped field values in the unmapped output field
        var configuration = context.getConfigurationOfExecutingNode();
        var staffTaskView = getStaffTaskView(context);
        var derivedRuntimeData = elementDerivationService
                .derive((GroupLayoutElement) staffTaskView.layout(), update);
        var effectiveValues = derivedRuntimeData.getEffectiveValues();
        var processDataUiDefinition = MODE_DATA.equals(configuration.contentMode)
                ? configuration.dataContent
                : null;
        var updatedProcessData = elementDataTransformService.buildUpdatedProcessData(
                processDataUiDefinition,
                derivedRuntimeData,
                context.getThisTask().getProcessData()
        );

        var nodeData = new HashMap<String, Object>();
        nodeData.put(OUTPUT_DECISION, decision);
        nodeData.put(OUTPUT_REMARK, remarkText);
        nodeData.put(OUTPUT_PROCESSED_BY_USER_ID, context.getCallingUser().getId());
        nodeData.put(OUTPUT_PROCESSED_AT, Instant.now());
        nodeData.put(OUTPUT_UNMAPPED, effectiveValues);

        var result = new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(port) // Set the desired output port
                .setNodeData(nodeData) // Set the generated node data
                .setRuntimeData(Map.of()) // Reset runtime data to empty map
                .setProcessData(updatedProcessData);

        StringBuilder logMessage = new StringBuilder();
        if (approved) {
            logMessage.append("Die Freigabe wurde durch %s erteilt.");
        } else {
            logMessage.append("Die Freigabe wurde durch %s abgelehnt.");
        }
        if (StringUtils.isNotNullOrEmpty(remarkText)) {
            logMessage
                    .append(" Der folgende Vermerk wurde angegeben: ")
                    .append(remarkText);
        }

        context
                .getLogger()
                .logf(
                        ProcessNodeExecutionLogLevel.Info,
                        false,
                        true,
                        true,
                        context.getThisTask().getAssignedUserId(),
                        null,
                        null,
                        approved ? "Freigabe erteilt" : "Freigabe abgelehnt",
                        Map.of(),
                        logMessage.toString(),
                        ProcessHistoryLabels.quotedUser(context.getCallingUser())
                );

        return Optional.of(result);
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
    private static Map<String, Object> extractWorkingProcessData(@Nonnull ProcessExecutionData processData)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        return new LinkedHashMap<>(processData.getProcessData());
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(ASSIGNMENT_CONTEXT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    @Override
    public Class<ApprovalConfiguration> getNodeConfigurationClass() {
        return ApprovalConfiguration.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class ApprovalConfiguration {
        public static final String CRITERIA = CRITERIA_FIELD_ID;
        @InputElementPOJOBinding(id = CRITERIA, type = ElementType.RichTextInput,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Freigabekriterien"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie, was die zuständige Person prüfen soll und unter welchen Voraussetzungen sie die Freigabe erteilen darf."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String criteria;

        public static final String CONTENT_MODE = CONTENT_MODE_FIELD_ID;
        @InputElementPOJOBinding(id = CONTENT_MODE, type = ElementType.Select, properties = {
                @ElementPOJOBindingProperty(key = "presentation", strValue = "radio"),
                @ElementPOJOBindingProperty(key = "label", strValue = "Zu prüfende Inhalte"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie, welche Informationen die zuständige Person für ihre Entscheidung erhält."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "toggleButtons", boolValue = true),
                @ElementPOJOBindingProperty(key = "displayInline", boolValue = true)
        })
        public String contentMode;

        public static final String DATA_CONTENT = DATA_CONTENT_FIELD_ID;
        @InputElementPOJOBinding(id = DATA_CONTENT, type = ElementType.UiDefinitionInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Ansicht für die Prüfung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Gestalten Sie mit einer UI-Definition die Felder und Hinweise für die Prüfung. Angaben aus Feldern mit Datenschlüssel werden bei Freigabe und Ablehnung in die Vorgangsdaten übernommen."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public GroupLayoutElement dataContent;

        public static final String CUSTOM_CONTENT = CUSTOM_CONTENT_FIELD_ID;
        @InputElementPOJOBinding(id = CUSTOM_CONTENT, type = ElementType.RichTextInput,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Text für die Prüfung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Beschreiben Sie die Inhalte, über die entschieden werden soll, zum Beispiel eine Prüfung in einem Fachsystem."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String customContent;

        public static final String ASSIGNMENT_CONTEXT = ASSIGNMENT_CONTEXT_FIELD_ID;
        @InputElementPOJOBinding(id = ASSIGNMENT_CONTEXT, type = ElementType.AssignmentContext, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "headline", strValue = "Verantwortlicher Personenkreis"),
                @ElementPOJOBindingProperty(key = "text", strValue = "Wählen Sie Organisationseinheiten, Teams oder Mitarbeiter:innen aus. Prosuna weist die Aufgabe einer berechtigten Person aus diesem Personenkreis zu."),
                @ElementPOJOBindingProperty(key = "placeholder", strValue = "Organisationseinheit, Team oder Mitarbeiter:in suchen"),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public AssignmentContextInputElementValue assignmentContext;
    }
}
