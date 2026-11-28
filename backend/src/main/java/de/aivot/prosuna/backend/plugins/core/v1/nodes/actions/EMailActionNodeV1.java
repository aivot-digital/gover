package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.enums.InputMode;
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
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.models.MailSendOptions;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.markdown.utils.MarkdownRenderer;
import de.aivot.prosuna.backend.models.lib.MailAttachmentBytes;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
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
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentService;
import de.aivot.prosuna.backend.process.services.ProcessInstanceAttachmentSetService;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.system.services.SystemService;
import de.aivot.prosuna.backend.theme.services.ThemeService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;

@Component
public class EMailActionNodeV1 implements ProcessNodeDefinition<EMailActionNodeV1.EMailActionNodeConfig> {
    private static final ProcessNodeDeadlineDefinition MESSAGE_DISPATCH_DEADLINE = MessageDispatchDeadline.DEFINITION;

    public static final String NODE_KEY = "mail";

    private static final String PORT_NAME = "output";

    private static final String OUTPUT_NAME_TO = "to";
    private static final String OUTPUT_NAME_BCC = "bcc";
    private static final String OUTPUT_NAME_SUBJECT = "subject";
    private static final String OUTPUT_NAME_CONTENT = "content";
    private static final String OUTPUT_NAME_ATTACHMENT_SET_DATA_KEYS = "attachmentSetDataKeys";

    private final MailService mailService;
    private final ProcessInstanceAttachmentService processInstanceAttachmentService;
    private final ProcessInstanceAttachmentSetService processInstanceAttachmentSetService;
    private final StorageService storageService;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private final ProcessService processService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final ThemeService themeService;
    private final SystemService systemService;

    public EMailActionNodeV1(MailService mailService,
                             ProcessInstanceAttachmentService processInstanceAttachmentService,
                             ProcessInstanceAttachmentSetService processInstanceAttachmentSetService,
                             StorageService storageService,
                             AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService,
                             ProcessService processService,
                             VDepartmentShadowedService vDepartmentShadowedService,
                             ThemeService themeService,
                             SystemService systemService) {
        this.mailService = mailService;
        this.processInstanceAttachmentService = processInstanceAttachmentService;
        this.processInstanceAttachmentSetService = processInstanceAttachmentSetService;
        this.storageService = storageService;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
        this.processService = processService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.themeService = themeService;
        this.systemService = systemService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<EMailActionNodeV1.EMailActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var recipients = summaryRecipients(summary.data(OUTPUT_NAME_TO));
        var bcc = summaryRecipients(summary.data(OUTPUT_NAME_BCC));
        var subject = StringUtils.toNullableTrimmedString(summary.data(OUTPUT_NAME_SUBJECT));
        var content = summary.data(OUTPUT_NAME_CONTENT);
        var description = StringUtils.toNullableTrimmedString(context.thisNode().getDescription());
        return "Die E-Mail wurde" + summary.completedAt() + " versendet."
                + (recipients == null ? "" : detail("E-Mail-Adressen", recipients))
                + (bcc == null ? "" : detail("Blindkopie (BCC)", bcc))
                + (subject == null ? "" : detail("Betreff der E-Mail", subject))
                + (content == null || content.toString().isBlank() ? "" : section("Nachricht", content))
                + (description == null ? "" : detail("Kurzbeschreibung", description));
    }

    @Nullable
    private static String summaryRecipients(@Nullable Object value) {
        final List<?> entries;
        if (value instanceof String[] array) {
            entries = Arrays.asList(array);
        } else if (value instanceof List<?> list) {
            entries = list;
        } else {
            return StringUtils.toNullableTrimmedString(value);
        }
        if (entries.stream().noneMatch(entry -> StringUtils.toNullableTrimmedString(entry) != null)) {
            return null;
        }
        // Rejoin stored fragments: legacy results also split commas inside quoted display names.
        return StringUtils.toNullableTrimmedString(String.join(",", entries.stream()
                .map(entry -> entry == null ? "" : entry.toString())
                .toList()));
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
                ProcessNodeExecutionType.SemiAutomatic,
        };
    }

    @Nonnull
    @Override
    public String getName() {
        return "E-Mail versenden";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Versendet eine E-Mail an festgelegte Adressen – automatisch oder nach Prüfung durch eine zuständige Person.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Versenden Sie eine E-Mail an angegebene Adressen, zum Beispiel eine Eingangsbestätigung oder ein erstelltes Dokument. Die E-Mail kann automatisch versendet oder vorher von einer zuständigen Person geprüft und bearbeitet werden.

                Für dieses Element ist keine Identität erforderlich. Soll die Nachricht über die bei einer Identität hinterlegte Kommunikationsanbindung versendet werden, verwenden Sie „Nachricht an Identität senden“.

                ### Was richten Sie ein?

                Geben Sie die E-Mail-Adressen an. Trennen Sie mehrere Adressen durch Kommas. Unter „Blindkopie (BCC)“ können Sie zusätzliche Adressen hinterlegen, die für die anderen empfangenden Personen und Organisationen nicht sichtbar sind. Angaben aus dem Vorgang können über Variablen eingefügt werden.

                Wählen Sie unter „Nachrichtenversand“ zwischen „Automatisch versenden“ und „Vor Versand prüfen und bearbeiten“. Hinterlegen Sie für den automatischen Versand einen Betreff und eine Nachricht. Für die manuelle Prüfung sind ein vorbelegter Betreff, eine vorbelegte Nachricht und der verantwortliche Personenkreis erforderlich. Die zuständige Person kann Betreff und Nachricht vor dem Versand ändern.

                Optional wählen Sie eine Organisationseinheit, deren Standard-E-Mail-Signatur ergänzt wird. Logo und Farben richten sich nach dem Erscheinungsbild der verwaltenden Organisationseinheit des Prozesses, ersatzweise nach dem Standarderscheinungsbild.

                Sollen Anlagen angehängt werden, wählen Sie die entsprechenden Anlagensätze des Vorgangs. Alle Anlagen dieser Sätze werden mit ihren vorhandenen Dateinamen angehängt. Ohne Auswahl wird die E-Mail ohne Anhänge versendet.

                ### Was passiert bei der Ausführung?

                Beim automatischen Versand stellt Prosuna die Nachricht aus den konfigurierten Angaben zusammen und übergibt sie mit den ausgewählten Anlagen an den Mailserver.

                Bei „Vor Versand prüfen und bearbeiten“ erhält eine berechtigte Person aus dem festgelegten Personenkreis eine Mitarbeitendenaufgabe. Sie prüft und bearbeitet Betreff und Nachricht und wählt anschließend „E-Mail versenden“. Empfangsadressen, Blindkopien und Anhänge werden aus der Konfiguration übernommen und können in dieser Aufgabe nicht geändert werden. Unvollständige Entwürfe können automatisch gespeichert werden; beim Versand sind Betreff und Nachricht erforderlich.

                Nach erfolgreicher Übergabe an den Mailserver wird der Vorgang über „E-Mail versendet“ fortgesetzt. Bei einem Fehler bleibt eine bereits zugewiesene Mitarbeitendenaufgabe offen. Bei automatischer Ausführung schlägt die Aufgabe fehl.

                Für die Mitarbeitendenaufgabe kann eine Versandfrist eingerichtet werden. Wird die Aufgabe bei Fristüberschreitung automatisch beendet, setzt Prosuna den Vorgang über „Versandfrist überschritten“ fort, ohne die E-Mail zu versenden.

                ### Welches Ergebnis steht danach bereit?

                Nach dem Versand stehen in den Elementdaten die Empfangsangaben, Blindkopien, der Betreff, die geschriebene Nachricht als Markdown und die Datenschlüssel der verwendeten Anlagensätze bereit. Die beim Versand ergänzte Signatur und Gestaltung gehören nicht zum gespeicherten Nachrichtentext. Das Element selbst verändert keine Vorgangsdaten.

                Im Vorgangsverlauf sehen Sie den abgeschlossenen Versand mit den gespeicherten Empfangsangaben, dem Betreff und der Nachricht. Ein automatisches Ende wegen Fristüberschreitung wird gesondert ausgewiesen.

                ### Wie sieht ein Beispiel aus?

                Eine Eingangsbestätigung soll an die unter `$.kontakt.email` gespeicherte Adresse gehen. Sie wählen diese Variable für „E-Mail-Adressen“, hinterlegen den Betreff „Ihre Angaben sind eingegangen“ und formulieren die Nachricht mit Angaben aus dem Vorgang. Zusätzlich wählen Sie den Anlagensatz „Erstellte Dokumente“ aus. Prosuna versendet die Nachricht mit allen Anlagen dieses Satzes. Bei manueller Prüfung kann die zuständige Person vorher den Betreff und die Nachricht anpassen.

                ### Was ist zu beachten?

                Der E-Mail-Versand muss für die Prosuna-Instanz eingerichtet sein. Ein ausgewählter Anlagensatz muss im Vorgang vorhanden sein und Anlagen enthalten; andernfalls wird der Versand abgebrochen.

                Dieses Element bestätigt die Übergabe an den Mailserver. Ob die E-Mail im Postfach angekommen oder gelesen wurde, prüft es nicht. Ein gemeldeter Versandfehler schließt eine bereits teilweise erfolgte Übermittlung nicht sicher aus.

                Hier wählen Sie vollständige Anlagensätze. Einzelne Dateien auswählen, für den Versand umbenennen oder in der Mitarbeitendenaufgabe neue Anhänge hochladen können Sie mit diesem Element derzeit nicht.
                """;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(EMailActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Einstellungen für „E-Mail versenden“ konnten nicht geladen werden.",
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
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull EMailActionNodeConfig configuration) {
        return SemiAutomaticMessageConfig.isManual(configuration.messageConfig)
                ? List.of(MESSAGE_DISPATCH_DEADLINE) : List.of();
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull EMailActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_NAME,
                        "E-Mail versendet",
                        "Der Vorgang wird hier fortgesetzt, nachdem die E-Mail an den Mailserver übergeben wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_NAME_TO,
                        "E-Mail-Adressen",
                        "Die beim Versand verwendeten Empfangsangaben.",
                        "Array<string>"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_BCC,
                        "Blindkopie (BCC)",
                        "Die beim Versand verwendeten Empfangsangaben für Blindkopien. Ohne Blindkopie bleibt dieser Wert leer.",
                        "Array<string> | null"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_SUBJECT,
                        "Betreff der E-Mail",
                        "Der Betreff der versendeten E-Mail.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_CONTENT,
                        "Nachricht",
                        "Die versendete Nachricht als Markdown, ohne die beim Versand ergänzte E-Mail-Signatur.",
                        "string",
                        true
                ),
                new ProcessNodeOutput(
                        OUTPUT_NAME_ATTACHMENT_SET_DATA_KEYS,
                        "Anlagensätze für Anhänge",
                        "Die Datenschlüssel der Anlagensätze, deren Anlagen an die E-Mail angehängt wurden.",
                        "Array<string>"
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<EMailActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();

        if (SemiAutomaticMessageConfig.isAutomatic(configuration.messageConfig)) {
            return initAutomatic(context, configuration);
        }
        if (SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            return initManual(context, configuration);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Wählen Sie, ob die E-Mail automatisch oder nach manueller Prüfung versendet werden soll."
        );
    }

    private ProcessNodeExecutionResult initAutomatic(@Nonnull ProcessNodeExecutionInitContext<EMailActionNodeConfig> context,
                                                     @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var automaticContent = requireAutomaticContent(config);
        var subject = automaticContent.subject.trim();
        var contentMarkdown = automaticContent.content.trim();

        return sendMail(subject,
                contentMarkdown,
                config,
                context.getThisProcessInstance());
    }

    private ProcessNodeExecutionResult initManual(@Nonnull ProcessNodeExecutionInitContext<EMailActionNodeConfig> context,
                                                  @Nonnull EMailActionNodeConfig config) throws ProcessNodeExecutionException {
        var manualContent = requireManualContent(config);
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
                        "Für die E-Mail-Aufgabe %s konnte keine berechtigte Person aus dem festgelegten Personenkreis ermittelt werden.",
                        StringUtils.quote(context.getThisNode().getName() != null ? context.getThisNode().getName() : getName())
                ));

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setNewDeadlineKey(MESSAGE_DISPATCH_DEADLINE.key())
                .setRuntimeData(new LinkedHashMap<>(context.getThisTask().getRuntimeData()))
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<EMailActionNodeConfig> context) throws ResponseException {
        var root = new GroupLayoutElement();
        root.setId("root");
        root.setChildren(new LinkedList<>());

        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der E-Mail");
        subjectField.setRequired(true);
        root.getChildren().add(subjectField);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Nachricht");
        contentField.setHint("Prüfen und bearbeiten Sie die Nachricht vor dem Versand.");
        contentField.setRequired(true);
        root.getChildren().add(contentField);

        var manualContent = requireManualContentForStaffView(context.getConfigurationOfExecutingNode());
        var taskViewData = new AuthoredElementValues();

        taskViewData.putLiteral(STAFF_TASK_SUBJECT_FIELD_ID, manualContent.subject);
        taskViewData.putLiteral(STAFF_TASK_CONTENT_FIELD_ID, manualContent.content);
        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("E-Mail versenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
    }

    private static final String STAFF_TASK_SEND_EVENT = "send";

    @Nonnull
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(@Nonnull ProcessNodeExecutionContextUIStaff<EMailActionNodeConfig> context,
                                                                         @Nonnull AuthoredElementValues update,
                                                                         @Nonnull String event) throws ResponseException, ProcessNodeExecutionException {
        if (!STAFF_TASK_SEND_EVENT.equals(event)) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    "Die ausgewählte Aktion ist für diese Aufgabe nicht verfügbar. Aktion: %s.",
                    StringUtils.quote(event)
            );
        }

        var config = context.getConfigurationOfExecutingNode();
        if (!SemiAutomaticMessageConfig.isManual(config.messageConfig)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die E-Mail kann nur bei teilautomatisierter Ausführung über eine Mitarbeitendenaufgabe versendet werden."
            );
        }

        var derivedRuntimeData = new DerivedRuntimeElementData();

        var subject = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_SUBJECT_FIELD_ID));
        if (subject == null) {
            derivedRuntimeData.getElementStates().put(STAFF_TASK_SUBJECT_FIELD_ID, new ComputedElementState()
                    .setError("Geben Sie einen Betreff für die E-Mail ein.")
            );
        }

        var content = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_CONTENT_FIELD_ID));
        if (content == null) {
            derivedRuntimeData.getElementStates().put(STAFF_TASK_CONTENT_FIELD_ID, new ComputedElementState()
                    .setError("Geben Sie eine Nachricht für die E-Mail ein.")
            );
        }

        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }

        var res = sendMail(subject,
                content,
                config,
                context.getThisProcessInstance());

        return Optional.of(res);
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
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull EMailActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Hinterlegen Sie einen Betreff und eine Nachricht für den automatischen E-Mail-Versand."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull EMailActionNodeConfig configuration
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
            @Nonnull EMailActionNodeConfig configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerErrorWithDetails(e, e.getMessage(), null);
        }
    }

    private ProcessNodeExecutionResult sendMail(@Nonnull String subject,
                                                @Nonnull String contentMarkdown,
                                                @Nonnull EMailActionNodeConfig config,
                                                @Nonnull ProcessInstanceEntity processInstance) throws ProcessNodeExecutionException {
        var recipientsStr = StringUtils.toNullableTrimmedString(config.to);

        if (recipientsStr == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Geben Sie mindestens eine E-Mail-Adresse an."
            );
        }
        var recipients = recipientsStr.split(",");

        var recipientsBccStr = StringUtils.toNullableTrimmedString(config.bcc);
        var recipientsBCC = recipientsBccStr == null ? null : recipientsBccStr.split(",");

        var attachmentSetDataKeys = config.attachmentSetDataKeys;
        if (attachmentSetDataKeys == null) {
            attachmentSetDataKeys = new ArrayList<>();
        }

        var contentHtml = MarkdownRenderer.renderHtml(contentMarkdown);

        if (StringUtils.isNullOrEmpty(contentHtml)) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Für den E-Mail-Versand ist eine Nachricht erforderlich."
            );
        }

        if (!mailService.isSendingConfigured()) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der E-Mail-Versand ist in Prosuna nicht eingerichtet."
            );
        }

        final var process = retrieveProcess(processInstance);
        final var signatureDepartment = SemiAutomaticMessageConfig.resolveSignatureDepartment(
                config.messageConfig,
                vDepartmentShadowedService
        );
        final var department = vDepartmentShadowedService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die verwaltende Organisationseinheit des Prozesses wurde nicht gefunden. Organisationseinheit: %d; Prozess: %d.",
                        process.getDepartmentId(),
                        process.getId()
                ));
        final var theme = department.getThemeId() == null
                ? systemService.retrieveDefaultTheme()
                : themeService
                        .retrieve(department.getThemeId())
                        .orElseGet(systemService::retrieveDefaultTheme);
        final var mailAttachments = loadMailAttachments(processInstance, attachmentSetDataKeys);

        var mailContext = new HashMap<String, Object>();
        mailContext.put("title", subject);
        mailContext.put("messageText", MarkdownRenderer.renderPlainText(contentMarkdown));
        mailContext.put("messageHtml", contentHtml);
        mailContext.put("department", department);
        if (signatureDepartment != null) {
            mailContext.put("signatureDepartment", signatureDepartment);
        }

        try {
            mailService.sendMail(
                    theme,
                    recipientsStr,
                    Optional.empty(),
                    Optional.ofNullable(recipientsBccStr),
                    subject,
                    MailTemplate.ProcessEmail,
                    mailContext,
                    Optional.empty(),
                    mailAttachments.isEmpty() ? Optional.empty() : Optional.of(mailAttachments),
                    new MailSendOptions(signatureDepartment != null)
            );
        } catch (MailException exception) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    exception,
                    "Beim Versand der E-Mail ist ein Fehler aufgetreten."
            );
        } catch (MessagingException | ResponseException | RuntimeException exception) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    exception,
                    "Beim Vorbereiten oder Versenden der E-Mail ist ein Fehler aufgetreten."
            );
        }

        var metadata = new HashMap<String, Object>();
        metadata.put(OUTPUT_NAME_TO, recipients);
        metadata.put(OUTPUT_NAME_BCC, recipientsBCC);
        metadata.put(OUTPUT_NAME_SUBJECT, subject);
        // Later steps insert the content into templates or pass it on, so it is the Markdown that was written, as for
        // Postfach messages. The HTML is only the mail's rendering of it.
        metadata.put(OUTPUT_NAME_CONTENT, contentMarkdown);
        metadata.put(OUTPUT_NAME_ATTACHMENT_SET_DATA_KEYS, attachmentSetDataKeys);

        return new ProcessNodeExecutionResultTaskCompleted()
                .setViaPort(PORT_NAME)
                .setNodeData(metadata);
    }

    @Nonnull
    private ProcessEntity retrieveProcess(
            @Nonnull ProcessInstanceEntity processInstance
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        try {
            return processService
                    .retrieve(processInstance.getProcessId())
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Prozess für den E-Mail-Versand wurde nicht gefunden. Prozess: %d.",
                            processInstance.getProcessId()
                    ));
        } catch (ResponseException exception) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    exception,
                    "Der Prozess für den E-Mail-Versand konnte nicht geladen werden. Prozess: %d.",
                    processInstance.getProcessId()
            );
        }
    }

    @Nonnull
    private List<MailAttachmentBytes> loadMailAttachments(
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull List<String> attachmentSetDataKeys
    ) throws ProcessNodeExecutionException {
        var mailAttachments = new ArrayList<MailAttachmentBytes>();
        for (var attachmentSetDataKey : attachmentSetDataKeys) {
            for (var attachment : resolveProcessAttachmentsBySetDataKey(processInstance, attachmentSetDataKey)) {
                try (var attachmentContent = storageService.getDocumentContent(
                        attachment.getStorageProviderId(),
                        attachment.getStoragePathFromRoot()
                )) {
                    var contentType = MediaTypeFactory
                            .getMediaType(attachment.getFileName())
                            .orElse(MediaType.APPLICATION_OCTET_STREAM);
                    mailAttachments.add(new MailAttachmentBytes(
                            attachment.getFileName(),
                            contentType,
                            attachmentContent.readAllBytes()
                    ));
                } catch (IOException | ResponseException exception) {
                    throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                            exception,
                            "Die Anlage %s konnte für den E-Mail-Versand nicht geladen werden.",
                            StringUtils.quote(attachment.getFileName())
                    );
                }
            }
        }
        return mailAttachments;
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2);
        return configuration;
    }

    @Nonnull
    private List<ProcessInstanceAttachmentEntity> resolveProcessAttachmentsBySetDataKey(@Nonnull ProcessInstanceEntity processInstance,
                                                                                        @Nonnull String attachmentSetDataKey) throws ProcessNodeExecutionException {
        var normalizedDataKey = StringUtils.toNullableTrimmedString(attachmentSetDataKey);
        if (normalizedDataKey == null) {
            return List.of();
        }

        var attachmentSets = processInstanceAttachmentSetService
                .findAllByProcessInstanceIdAndDataKey(processInstance.getId(), normalizedDataKey);

        if (attachmentSets.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s wurde im Vorgang %d nicht gefunden.",
                    StringUtils.quote(normalizedDataKey),
                    processInstance.getId()
            );
        }

        var attachments = new ArrayList<ProcessInstanceAttachmentEntity>();
        for (var attachmentSet : attachmentSets) {
            attachments.addAll(processInstanceAttachmentService.findAllByAttachmentSetId(attachmentSet.getId()));
        }

        if (attachments.isEmpty()) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Der Anlagensatz mit dem Datenschlüssel %s enthält keine Anlagen.",
                    StringUtils.quote(normalizedDataKey)
            );
        }

        return attachments;
    }

    @Nonnull
    @Override
    public Class<EMailActionNodeConfig> getNodeConfigurationClass() {
        return EMailActionNodeConfig.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class EMailActionNodeConfig {
        public static final String RECIPIENT_FIELD_ID = "to";
        public static final String BCC_RECIPIENT_FIELD_ID = "bcc";
        public static final String ATTACHMENT_SET_DATA_KEYS_FIELD_ID = "attachment_file_names";

        @InputElementPOJOBinding(id = RECIPIENT_FIELD_ID, type = ElementType.Text,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "E-Mail-Adressen"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie die E-Mail-Adressen an, an die die Nachricht versendet werden soll. Trennen Sie mehrere Adressen durch Kommas. Angaben aus dem Vorgang können über Variablen eingefügt werden."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String to;

        @InputElementPOJOBinding(id = BCC_RECIPIENT_FIELD_ID, type = ElementType.Text,
                dynamicText = true,
                allowedInputModes = {InputMode.Literal, InputMode.Variable, InputMode.NoCode, InputMode.LowCode}, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Blindkopie (BCC)"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Geben Sie zusätzliche E-Mail-Adressen für eine Blindkopie an. Trennen Sie mehrere Adressen durch Kommas. Diese Adressen sind für die anderen empfangenden Personen und Organisationen nicht sichtbar."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public String bcc;

        @InputElementPOJOBinding(id = ATTACHMENT_SET_DATA_KEYS_FIELD_ID, type = ElementType.ProcessInstanceAttachmentSetSelect, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Anlagensätze für Anhänge"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Anlagensätze aus, deren Anlagen vollständig an die E-Mail angehängt werden sollen. Ohne Auswahl wird die E-Mail ohne Anhänge versendet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = false)
        })
        public List<String> attachmentSetDataKeys;

        /**
         * Dispatch mode, message templates and optional staff assignment.
         */
        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
