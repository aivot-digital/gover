package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.communication.models.ByteArrayCommunicationMessageAttachment;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageAttachment;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.markdown.utils.MarkdownRenderer;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionException;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidAssignment;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultCommunicationRequest;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.process.utils.ProcessHistoryLabels;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLConnection;
import java.time.Instant;
import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.timestamp;

/**
 * Sends one synchronous message through the communication provider selected for an identity.
 */
@Component
public class CommunicationMessageActionNodeV1 implements ProcessNodeDefinition<CommunicationMessageActionNodeV1.Configuration> {
    private static final ProcessNodeDeadlineDefinition MESSAGE_DISPATCH_DEADLINE = MessageDispatchDeadline.DEFINITION;

    public static final String NODE_KEY = "communication_message";
    private static final String PORT_OUTPUT = "output";
    private static final String OUTPUT_IDENTITY_ID = "identityId";
    private static final String OUTPUT_BINDING_ID = "communicationProviderBindingId";
    private static final String OUTPUT_SUBJECT = "subject";
    private static final String OUTPUT_BODY = "body";
    private static final String OUTPUT_ATTACHMENT_SET_DATA_KEYS = "attachmentSetDataKeys";
    private static final String OUTPUT_SENT_AT = "sentAt";
    private static final String OUTPUT_SEND_RESULT = "sendResult";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_SEND_EVENT = "send";

    private final ProcessInstanceAttachmentSetService attachmentSetService;
    private final ProcessInstanceAttachmentService attachmentService;
    private final StorageService storageService;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private final VDepartmentShadowedService vDepartmentShadowedService;

    public CommunicationMessageActionNodeV1(
            ProcessInstanceAttachmentSetService attachmentSetService,
            ProcessInstanceAttachmentService attachmentService,
            StorageService storageService,
            AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService,
            VDepartmentShadowedService vDepartmentShadowedService
    ) {
        this.attachmentSetService = attachmentSetService;
        this.attachmentService = attachmentService;
        this.storageService = storageService;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<CommunicationMessageActionNodeV1.Configuration> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var sentAt = summary.metadata(ExecutionSummaryMarkdown.SENT_AT);
        var identityId = StringUtils.toNullableTrimmedString(summary.data(OUTPUT_IDENTITY_ID));
        return "Die Nachricht wurde" + summary.at(timestamp(sentAt).isEmpty() ? context.thisTask().getFinished() : sentAt) + " versendet."
                + (identityId == null ? "" : section("Empfangende Identität", summary.identity(identityId)))
                + detail("Zustellweg", summary.metadata(ExecutionSummaryMarkdown.DELIVERY_CHANNEL))
                + detail("Betreff der Nachricht", summary.data(OUTPUT_SUBJECT))
                + section("Nachricht", summary.data(OUTPUT_BODY))
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
        return new ProcessNodeExecutionType[]{
                ProcessNodeExecutionType.Automatic,
                ProcessNodeExecutionType.SemiAutomatic
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return "Nachricht an Identität senden";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Versendet eine Nachricht über den hinterlegten Zustellweg einer Identität – automatisch oder nach manueller Prüfung.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Versenden Sie eine Nachricht oder Unterlagen an eine Identität des Vorgangs. Eine Identität enthält die zu diesem Vorgang gespeicherten Angaben zu einer beteiligten Person oder Organisation. Prosuna verwendet den für diese Identität hinterlegten Zustellweg.

                Für den ausdrücklichen Versand an angegebene E-Mail-Adressen verwenden Sie „E-Mail versenden“.

                ### Was richten Sie ein?

                Wählen Sie unter „Empfangende Identität“ genau eine Identität aus den vorhergehenden Prozessschritten aus. Sie muss im konkreten Vorgang bereits vorhanden sein und einen verwendbaren Zustellweg besitzen. Dieses Element legt keine neue Identität an.

                Wählen Sie unter „Nachrichtenversand“ zwischen „Automatisch versenden“ und „Vor Versand prüfen und bearbeiten“. Hinterlegen Sie für den automatischen Versand einen Betreff und eine Nachricht. Für die manuelle Prüfung sind ein vorbelegter Betreff, eine vorbelegte Nachricht und der verantwortliche Personenkreis erforderlich. Angaben aus dem Vorgang können über Variablen in Betreff und Nachricht eingefügt werden.

                Optional wählen Sie Anlagensätze aus. Alle Anlagen dieser Sätze werden mit ihren vorhandenen Dateinamen versendet. Ohne Auswahl wird die Nachricht ohne Anhänge versendet.

                Für den Versand per E-Mail können Sie eine Organisationseinheit für die Standard-E-Mail-Signatur auswählen. Deren Erscheinungsbild wird dabei ebenfalls berücksichtigt. Ohne diese Auswahl wird das Erscheinungsbild der verwaltenden Organisationseinheit des Prozesses beziehungsweise das Standarderscheinungsbild verwendet.

                ### Was passiert bei der Ausführung?

                Beim automatischen Versand stellt Prosuna die Nachricht aus den konfigurierten Angaben zusammen, lädt die ausgewählten Anlagen und versendet die Nachricht über den hinterlegten Zustellweg.

                Bei „Vor Versand prüfen und bearbeiten“ erhält eine berechtigte Person aus dem festgelegten Personenkreis eine Mitarbeitendenaufgabe. Sie prüft und bearbeitet Betreff und Nachricht und wählt anschließend „Nachricht versenden“. Die empfangende Identität und die Auswahl der Anhänge können in dieser Aufgabe nicht geändert werden. Unvollständige Entwürfe können automatisch gespeichert werden; beim Versand sind Betreff und Nachricht erforderlich.

                Eine E-Mail-Identität wird direkt über den zentralen E-Mail-Versand erreicht. Bei einer Identität eines Identitätsanbieters verwendet Prosuna die dort ausgewählte Kommunikationsanbindung. Nach erfolgreichem Versand wird der Vorgang über „Nachricht versendet“ fortgesetzt. Bei einem Fehler bleibt eine bereits zugewiesene Mitarbeitendenaufgabe offen. Bei automatischer Ausführung schlägt die Aufgabe fehl.

                Für die Mitarbeitendenaufgabe kann eine Versandfrist eingerichtet werden. Wird die Aufgabe bei Fristüberschreitung automatisch beendet, setzt Prosuna den Vorgang über „Versandfrist überschritten“ fort, ohne die Nachricht zu versenden.

                ### Welches Ergebnis steht danach bereit?

                In den Elementdaten stehen die empfangende Identität, die verwendete Kommunikationsanbindung, der Betreff, die geschriebene Nachricht als Markdown, die Datenschlüssel der verwendeten Anlagensätze, der Vorbereitungszeitpunkt und das Versandergebnis bereit. Welche Angaben das Versandergebnis enthält, hängt vom Zustellweg ab. Die fertige Darstellung einschließlich einer ergänzten Signatur gehört nicht zum gespeicherten Nachrichtentext. Das Element selbst verändert keine Vorgangsdaten.

                Der Vorgangsverlauf unterscheidet Vorbereitung und Versand. Die Zusammenfassung nennt den Versandzeitpunkt, die empfangende Identität, den Zustellweg, den Betreff und die Nachricht. Ein automatisches Ende wegen Fristüberschreitung wird gesondert ausgewiesen.

                ### Wie sieht ein Beispiel aus?

                Beim Formulareingang wurde die Identität „Kontakt“ angelegt. Sie wählen diese als empfangende Identität, hinterlegen den Betreff „Ihre Unterlagen“ und wählen den Anlagensatz „Erstellte Dokumente“. Prosuna versendet die Nachricht mit allen Anlagen dieses Satzes über den bei „Kontakt“ hinterlegten Zustellweg. Bei manueller Prüfung kann die zuständige Person vorher den Betreff und die Nachricht anpassen.

                ### Was ist zu beachten?

                Eine Identität eines Identitätsanbieters benötigt eine passende und verwendbare Kommunikationsanbindung. Ist diese nicht verfügbar, wechselt Prosuna nicht automatisch zu E-Mail. Für eine E-Mail-Identität muss der E-Mail-Versand der Prosuna-Instanz eingerichtet sein.

                Ein ausgewählter Anlagensatz muss im Vorgang vorhanden sein und Anlagen enthalten; andernfalls wird der Versand abgebrochen. Hier wählen Sie vollständige Anlagensätze. Einzelne Dateien auswählen, für den Versand umbenennen oder in der Mitarbeitendenaufgabe neue Anhänge hochladen können Sie mit diesem Element derzeit nicht.

                „Versendet“ bestätigt die erfolgreiche Rückmeldung des verwendeten Versanddienstes. Ob die Nachricht im Zielpostfach angekommen oder gelesen wurde, prüft dieses Element nicht. Ein gemeldeter Versandfehler schließt eine bereits teilweise erfolgte Übermittlung nicht sicher aus.
                """;
    }

    @Nonnull
    @Override
    public Class<Configuration> getNodeConfigurationClass() {
        return Configuration.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(
            @Nonnull ProcessNodeDefinitionConfigurationLayoutContext context
    ) throws ResponseException {
        final ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(Configuration.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Einstellungen für „Nachricht an Identität senden“ konnten nicht geladen werden.",
                    null
            );
        }

        layout.findChild(SemiAutomaticMessageConfig.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> SemiAutomaticMessageConfig.initConfigurationLayout(
                        group,
                        context.thisNode().getProcessId(),
                        context.thisNode().getProcessVersion()
                ));

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull Configuration configuration) {
        return SemiAutomaticMessageConfig.isManual(configuration.messageConfig)
                ? List.of(MESSAGE_DISPATCH_DEADLINE) : List.of();
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull Configuration configuration) {
        return List.of(new ProcessNodePort(
                PORT_OUTPUT,
                "Nachricht versendet",
                "Der Vorgang wird hier fortgesetzt, nachdem die Nachricht über den hinterlegten Zustellweg versendet wurde."
        ));
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(OUTPUT_IDENTITY_ID, "Empfangende Identität", "Der Schlüssel der Identität im Vorgang, an die die Nachricht versendet wurde.", "string"),
                new ProcessNodeOutput(OUTPUT_BINDING_ID, "Kommunikationsanbindung", "Die Kennung der in der Identität hinterlegten Kommunikationsanbindung. Bei einer E-Mail-Identität kann dieser Wert leer sein.", "number | null"),
                new ProcessNodeOutput(OUTPUT_SUBJECT, "Betreff der Nachricht", "Der Betreff der versendeten Nachricht.", "string"),
                new ProcessNodeOutput(OUTPUT_BODY, "Nachricht", "Die versendete Nachricht als Markdown, ohne eine beim Versand ergänzte Signatur.", "string", true),
                new ProcessNodeOutput(OUTPUT_ATTACHMENT_SET_DATA_KEYS, "Anlagensätze für Anhänge", "Die Datenschlüssel der Anlagensätze, deren Anlagen mit der Nachricht versendet wurden.", "Array<string>"),
                new ProcessNodeOutput(OUTPUT_SENT_AT, "Vorbereitet am", "Der Zeitpunkt, zu dem die Nachricht für den Versand vorbereitet wurde. Der tatsächliche Versand folgt danach.", "string"),
                new ProcessNodeOutput(OUTPUT_SEND_RESULT, "Versandergebnis", "Die beim Versand zurückgegebenen Angaben. Inhalt und Bedeutung hängen vom verwendeten Zustellweg ab.", "Record<string, unknown>")
        );
    }

    @Override
    public ProcessNodeExecutionResult init(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context
    ) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        if (SemiAutomaticMessageConfig.isAutomatic(configuration.messageConfig)) {
            return initAutomatic(context, configuration);
        }
        if (SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            return initManual(context, configuration);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Wählen Sie, ob die Nachricht automatisch oder nach manueller Prüfung versendet werden soll."
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initAutomatic(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context,
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionException {
        var automaticContent = requireAutomaticContent(configuration);
        var subject = automaticContent.subject.trim();
        var content = automaticContent.content.trim();

        return createCommunicationResult(
                context.getLogger(),
                configuration,
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                context.getThisTask(),
                subject,
                content
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initManual(
            @Nonnull ProcessNodeExecutionInitContext<Configuration> context,
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionException {
        var manualContent = requireManualContent(configuration);
        var assigneeUserId = assignmentContextAssigneeResolverService
                .resolveAssignee(
                        context.getThisNode().getProcessId(),
                        context.getThisNode().getProcessVersion(),
                        context.getThisProcessInstance().getId(),
                        context.getThisNode().getId(),
                        context.getThisTask().getId(),
                        context.getThisTask().getPreviousProcessNodeId(),
                        context.getThisProcessInstance().getAssignedUserId(),
                        manualContent.assignmentContext,
                        List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK)
                )
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidAssignment(
                        "Für die Nachrichtenaufgabe %s konnte keine berechtigte Person aus dem festgelegten Personenkreis ermittelt werden.",
                        StringUtils.quote(context.getThisNode().resolveName(this))
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setNewDeadlineKey(MESSAGE_DISPATCH_DEADLINE.key())
                .setRuntimeData(new LinkedHashMap<>(context.getThisTask().getRuntimeData()))
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<Configuration> context
    ) throws ResponseException {
        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der Nachricht");
        subjectField.setRequired(true);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Nachricht");
        contentField.setHint("Prüfen und bearbeiten Sie die Nachricht vor dem Versand.");
        contentField.setRequired(true);

        var root = new GroupLayoutElement();
        root.setId(STAFF_TASK_ROOT_ID);
        root.setChildren(new LinkedList<>(List.of(subjectField, contentField)));

        var manualContent = requireManualContentForStaffView(context.getConfigurationOfExecutingNode());
        var taskViewData = new AuthoredElementValues();

        taskViewData.putLiteral(STAFF_TASK_SUBJECT_FIELD_ID, manualContent.subject.trim());
        taskViewData.putLiteral(STAFF_TASK_CONTENT_FIELD_ID, manualContent.content.trim());

        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("Nachricht versenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
    }

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<Configuration> context,
            @Nonnull AuthoredElementValues update,
            @Nonnull String event
    ) throws ResponseException, ProcessNodeExecutionException {
        if (!STAFF_TASK_SEND_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Die ausgewählte Aktion ist für diese Aufgabe nicht verfügbar. Aktion: %s.",
                    StringUtils.quote(event)
            );
        }

        var configuration = context.getConfigurationOfExecutingNode();
        if (!SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Nachricht kann nur bei teilautomatisierter Ausführung über eine Mitarbeitendenaufgabe versendet werden."
            );
        }

        var subject = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_SUBJECT_FIELD_ID));
        var content = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_CONTENT_FIELD_ID));
        validateStaffMessage(subject, content);

        context
                .getLogger()
                .logf(
                        ProcessNodeExecutionLogLevel.Info,
                        false,
                        true,
                        true,
                        context.getCallingUser().getId(),
                        null,
                        null,
                        "Versand angefordert",
                        Map.of(),
                        "Der Versand der Nachricht mit dem Betreff %s wurde durch %s angefordert.",
                        StringUtils.quote(subject),
                        ProcessHistoryLabels.quotedUser(context.getCallingUser())
                );

        return Optional.of(createCommunicationResult(
                context.getLogger(),
                configuration,
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                context.getThisTask(),
                subject,
                content
        ));
    }

    @Nonnull
    @Override
    public ResponseException unavailableStaffTaskEvent(@Nonnull String event, @Nonnull AuthoredElementValues inputs) {
        return ResponseException.badRequest(
                "Die ausgewählte Aktion ist für diese Aufgabe nicht verfügbar. Aktion: %s.",
                StringUtils.quote(event)
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult createCommunicationResult(
            @Nonnull ProcessNodeExecutionLogger logger,
            @Nonnull Configuration configuration,
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity task,
            @Nonnull String subject,
            @Nonnull String content
    ) throws ProcessNodeExecutionException {
        var identityId = requireIdentityId(configuration.identityId);
        var identity = processInstance.getIdentities().get(identityId);
        if (identity == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die ausgewählte Identität %s ist im Vorgang nicht vorhanden.",
                    StringUtils.quote(identityId)
            );
        }

        var attachmentSetDataKeys = configuration.attachmentSetDataKeys == null
                ? List.<String>of()
                : configuration.attachmentSetDataKeys;
        var attachments = resolveAttachments(processInstance, attachmentSetDataKeys);
        var sentAt = Instant.now();

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_IDENTITY_ID, identityId);
        nodeData.put(OUTPUT_BINDING_ID, identity.communicationProviderBindingId());
        nodeData.put(OUTPUT_SUBJECT, subject);
        nodeData.put(OUTPUT_BODY, content);
        nodeData.put(OUTPUT_ATTACHMENT_SET_DATA_KEYS, attachmentSetDataKeys);
        nodeData.put(OUTPUT_SENT_AT, sentAt);

        logger
                .logf(
                        ProcessNodeExecutionLogLevel.Info,
                        false,
                        true,
                        true,
                        task.getAssignedUserId(),
                        identityId,
                        identity.title(),
                        "Versand vorbereitet",
                        Map.of(),
                        "Der Versand der Nachricht mit dem Betreff %s an die Identität %s wurde vorbereitet.",
                        StringUtils.quote(subject),
                        StringUtils.quote(ProcessHistoryLabels.nameOrId(identityId, identity.title()))
                );

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_OUTPUT)
                .setProcessData(processExecutionData.getProcessData())
                .setNodeData(nodeData)
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                        identityId,
                        new CommunicationMessage.Builder()
                                .subject(subject)
                                .body(MarkdownRenderer.renderPlainText(content))
                                .markdownBody(content)
                                .timestamp(sentAt)
                                .attachments(attachments)
                                .signatureDepartment(SemiAutomaticMessageConfig.resolveSignatureDepartment(
                                        configuration.messageConfig,
                                        vDepartmentShadowedService
                                ))
                                .build(),
                        OUTPUT_SEND_RESULT
                ));
    }

    @Nonnull
    private String requireIdentityId(@Nullable String rawIdentityId)
            throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var identityId = StringUtils.toNullableTrimmedString(rawIdentityId);
        if (identityId == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Wählen Sie eine empfangende Identität aus."
            );
        }
        return identityId;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Hinterlegen Sie einen Betreff und eine Nachricht für den automatischen Versand."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull Configuration configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.manualContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Hinterlegen Sie einen vorbelegten Betreff und eine vorbelegte Nachricht für die manuelle Prüfung."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContentForStaffView(
            @Nonnull Configuration configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerErrorWithDetails(e, e.getMessage(), null);
        }
    }

    private static void validateStaffMessage(
            @Nullable String subject,
            @Nullable String content
    ) throws ResponseException {
        var derivedRuntimeData = new DerivedRuntimeElementData();
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_SUBJECT_FIELD_ID,
                    new ComputedElementState().setError("Geben Sie einen Betreff für die Nachricht ein.")
            );
        }
        if (content == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    new ComputedElementState().setError("Geben Sie eine Nachricht ein.")
            );
        }
        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }
    }

    @Nonnull
    private List<CommunicationMessageAttachment> resolveAttachments(
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull List<String> attachmentSetDataKeys
    ) throws ProcessNodeExecutionException {
        var resolved = new ArrayList<CommunicationMessageAttachment>();
        for (var rawDataKey : attachmentSetDataKeys) {
            var dataKey = StringUtils.toNullableTrimmedString(rawDataKey);
            if (dataKey == null) {
                continue;
            }

            var sets = attachmentSetService.findAllByProcessInstanceIdAndDataKey(processInstance.getId(), dataKey);
            if (sets.isEmpty()) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der konfigurierte Anlagensatz %s wurde nicht gefunden.",
                        StringUtils.quote(dataKey)
                );
            }

            var entities = new ArrayList<ProcessInstanceAttachmentEntity>();
            for (var set : sets) {
                entities.addAll(attachmentService.findAllByAttachmentSetId(set.getId()));
            }
            if (entities.isEmpty()) {
                throw new ProcessNodeExecutionExceptionMissingValue(
                        "Der konfigurierte Anlagensatz %s enthält keine Anlagen.",
                        StringUtils.quote(dataKey)
                );
            }

            for (var entity : entities) {
                try (var attachmentContent = storageService.getDocumentContent(
                        entity.getStorageProviderId(),
                        entity.getStoragePathFromRoot()
                )) {
                    resolved.add(new ByteArrayCommunicationMessageAttachment(
                            entity.getFileName(),
                            URLConnection.guessContentTypeFromName(entity.getFileName()),
                            attachmentContent.readAllBytes()
                    ));
                } catch (IOException | ResponseException e) {
                    throw new ProcessNodeExecutionExceptionUnknown(
                            e,
                            "Die Anlage %s konnte für den Nachrichtenversand nicht geladen werden.",
                            StringUtils.quote(entity.getFileName())
                    );
                }
            }
        }
        return resolved;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(Configuration.IDENTITY_ID_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2);
        return configuration;
    }

    /**
     * Configuration shown in the process-node editor.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class Configuration {
        public static final String IDENTITY_ID_FIELD_ID = "identityId";
        public static final String ATTACHMENTS_FIELD_ID = "attachmentSetDataKeys";

        /**
         * Logical process identity receiving the message. Options come from incoming node metadata;
         * a null or blank value makes the node configuration invalid at execution time. Every configured provider
         * option must have a usable communication binding because the message is sent through the selected identity.
         */
        @InputElementPOJOBinding(id = IDENTITY_ID_FIELD_ID, type = ElementType.ProcessIdentityIdInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfangende Identität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Identität des Vorgangs aus, an die die Nachricht versendet werden soll. Prosuna verwendet den für diese Identität hinterlegten Zustellweg."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "requiresCommunication", boolValue = true)
        })
        @Nullable
        public String identityId;

        /**
         * Optional process attachment sets whose files are included in the outgoing message. A null list
         * is normalized to an empty list; configured missing or empty sets fail execution.
         */
        @InputElementPOJOBinding(id = ATTACHMENTS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensätze für Anhänge"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Anlagensätze aus, deren Anlagen vollständig mit der Nachricht versendet werden sollen. Ohne Auswahl wird die Nachricht ohne Anhänge versendet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        @Nullable
        public List<String> attachmentSetDataKeys;

        /**
         * Dispatch mode, message templates and optional staff assignment.
         */
        @Nullable
        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
