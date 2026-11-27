package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.zxing.WriterException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.models.CommunicationMessageCallToAction;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.department.services.VDepartmentShadowedService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ComputedElementState;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.prosuna.backend.elements.uiPresets.SemiAutomaticMessageConfig;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.markdown.utils.MarkdownRenderer;
import de.aivot.prosuna.backend.markdown.utils.MarkdownTemplateValues;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTaskAttemptService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.enums.TemplateOutputFormat;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionSummaryContext;
import de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultCommunicationRequest;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultNoop;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskAssigned;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUIStaff;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.permissions.ProcessPermissionProvider;
import de.aivot.prosuna.backend.process.services.AssignmentContextAssigneeResolverService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.text;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.section;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.detail;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.timestamp;
import static de.aivot.prosuna.backend.process.utils.ExecutionSummaryMarkdown.map;

@Component
public class PaymentRequestActionNodeV1 implements ProcessNodeDefinition<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> {
    private static final ProcessNodeDeadlineDefinition MESSAGE_DISPATCH_DEADLINE = MessageDispatchDeadline.DEFINITION;
    private static final ProcessNodeDeadlineDefinition PAYMENT_DEADLINE = new ProcessNodeDeadlineDefinition(
            "payment", "Zahlung",
            "Zeit zur Zahlung ab Versand der Zahlungsaufforderung.",
            new ProcessNodePort("payment_overdue", "Fristüberschreitung: Zahlung",
                    "Fortsetzung, wenn die Aufgabe wegen Überschreitung der Frist für die Zahlung beendet wurde."));

    public static final String NODE_KEY = "payment_request";

    private static final String PORT_PAID = "paid";

    private static final String OUTPUT_RECIPIENT_IDENTITY_ID = "recipientIdentityId";
    private static final String OUTPUT_PAYMENT_URL = "paymentUrl";
    private static final String OUTPUT_PAYMENT_PROVIDER_NAME = "paymentProviderName";
    private static final String OUTPUT_PAYMENT_TRANSACTION_KEY = "paymentTransactionKey";
    private static final String OUTPUT_PAYMENT_PURPOSE = "paymentPurpose";
    private static final String OUTPUT_PAYMENT_DESCRIPTION = "paymentDescription";
    private static final String OUTPUT_PAYMENT_TOTAL = "paymentTotal";
    private static final String OUTPUT_PAYMENT_STATUS = "paymentStatus";
    private static final String OUTPUT_PAYMENT_DETAILS = "paymentDetails";
    private static final String OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION =
            "\"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\"";
    private static final String OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION =
            "{ transactionUrl: string | null; transactionRedirectUrl: string | null; " +
                    "transactionId: string | null; transactionReference: string | null; " +
                    "transactionTimestamp: string | null; " +
                    "paymentMethod: \"GIROPAY\" | \"PAYDIRECT\" | \"CREDITCARD\" | \"PAYPAL\" | \"OTHER\" | null; " +
                    "paymentMethodDetail: string | null; " +
                    "status: " + OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION + " | null; " +
                    "statusDetail: string | null; } | null";

    private static final String STAFF_TASK_ROOT_ID = "root";
    private static final String STAFF_TASK_PAYMENT_INFORMATION_ID = "payment-information";
    private static final String STAFF_TASK_SUBJECT_FIELD_ID = "subject";
    private static final String STAFF_TASK_CONTENT_FIELD_ID = "body";
    private static final String STAFF_TASK_SEND_EVENT = "send";

    private final PaymentPayloadCreationService paymentPayloadCreationService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final TemplateRenderService templateRenderService;
    private final ProsunaConfig prosunaConfig;
    private final JsonMapper jsonMapper;
    private final AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final PaymentTaskAttemptService paymentTaskAttemptService;

    public PaymentRequestActionNodeV1(PaymentPayloadCreationService paymentPayloadCreationService,
                                      PaymentTransactionService paymentTransactionService,
                                      PaymentProviderRepository paymentProviderRepository,
                                      PaymentProviderDefinitionsService paymentProviderDefinitionsService,
                                      TemplateRenderService templateRenderService,
                                      ProsunaConfig prosunaConfig,
                                      JsonMapper jsonMapper,
                                      AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService,
                                      VDepartmentShadowedService vDepartmentShadowedService,
                                      PaymentTaskAttemptService paymentTaskAttemptService) {
        this.paymentPayloadCreationService = paymentPayloadCreationService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
        this.templateRenderService = templateRenderService;
        this.prosunaConfig = prosunaConfig;
        this.jsonMapper = jsonMapper;
        this.assignmentContextAssigneeResolverService = assignmentContextAssigneeResolverService;
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.paymentTaskAttemptService = paymentTaskAttemptService;
    }

    @Nonnull
    @Override
    public String generateExecutionSummary(@Nonnull ProcessNodeExecutionSummaryContext<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> context) {
        var summary = new ExecutionSummaryMarkdown(context);
        var information = summary.data(OUTPUT_PAYMENT_DETAILS);
        var reference = information instanceof XBezahldienstePaymentInformation details
                ? details.getTransactionReference() : map(information).get("transactionReference");
        var recipient = summary.identity(summary.data(OUTPUT_RECIPIENT_IDENTITY_ID));
        var sentAt = summary.metadata(ExecutionSummaryMarkdown.SENT_AT);
        var dispatch = timestamp(sentAt).isEmpty()
                ? "Die Zahlungsaufforderung richtet sich an " + recipient + "."
                : "Die Zahlungsaufforderung wurde" + summary.at(sentAt) + " an " + recipient + summary.delivery() + " versendet.";
        return dispatch
                + " Die Aufgabe wurde" + summary.completedAt() + " mit bestätigter Zahlung abgeschlossen."
                + detail("Verwendungszweck", summary.data(OUTPUT_PAYMENT_PURPOSE))
                + detail("Beschreibung", summary.data(OUTPUT_PAYMENT_DESCRIPTION))
                + detail("Gesamtbetrag (Euro)", summary.data(OUTPUT_PAYMENT_TOTAL) instanceof Number total
                        ? NumberUtils.formatGermanNumber(new BigDecimal(total.toString()), 2) : summary.data(OUTPUT_PAYMENT_TOTAL))
                + detail("Zahlungsanbieter", summary.data(OUTPUT_PAYMENT_PROVIDER_NAME))
                + detail("Zahlungsreferenz", reference)
                + (PAYMENT_DEADLINE.key().equals(context.thisTask().getDeadlineKey())
                        ? detail("Zahlungsfrist", timestamp(context.thisTask().getDeadline())) : "")
                + summaryPaymentItems(map(context.thisTask().getRuntimeData()).get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD))
                + detail("Kurzbeschreibung", context.thisNode().getDescription());
    }

    @Nonnull
    private String summaryPaymentItems(@Nullable Object rawPayload) {
        if (rawPayload == null) {
            return "";
        }
        try {
            var payload = rawPayload instanceof PaymentPayload typed ? typed : jsonMapper.convertValue(rawPayload, PaymentPayload.class);
            if (payload.getPaymentItems() == null || payload.getPaymentItems().isEmpty()) {
                return "";
            }
            var items = payload.getPaymentItems().stream()
                    .map(item -> "- " + text(item.getDescription()) + ": "
                            + NumberUtils.formatGermanNumber(item.getTotalPrice(), 2) + " Euro"
                            + (item.getTaxRate().signum() > 0
                            ? " (inkl. " + formatTaxRate(item.getTaxRate()) + " % Steuern)" : ""))
                    .collect(Collectors.joining("\n"));
            return section("Zahlungspositionen", items);
        } catch (RuntimeException ignored) {
            // Older task payloads may be incomplete; retain the confirmed payment and its total.
            return "";
        }
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
        return "Zahlung anfordern";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Fordert eine Online-Zahlung an und wartet auf die Zahlungsbestätigung.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return """
                ### Wofür verwenden Sie dieses Prozesselement?

                Fordern Sie Beteiligte dazu auf, einen Betrag über einen eingerichteten Zahlungsanbieter online zu bezahlen. Prosuna versendet eine Zahlungsaufforderung und setzt den Vorgang fort, sobald die Zahlung bestätigt ist.

                ### Was richten Sie ein?

                Wählen Sie die empfangende Identität des Vorgangs und den Zahlungsanbieter aus. Legen Sie Verwendungszweck, Beschreibung und Zahlungspositionen fest. Für die Positionen geben Sie Einzelpreis, Menge und Steuersatz an. Einzelpreise und Mengen können Sie auch mit No-Code oder Low-Code berechnen. Angaben aus dem Vorgang können in die dafür vorgesehenen Zahlungsfelder eingefügt werden.

                Unter „Nachrichtenversand“ wählen Sie „Automatisch versenden“ oder „Vor Versand prüfen und bearbeiten“. Beim automatischen Versand legen Sie Betreff und Nachricht fest. Bei vorheriger Prüfung hinterlegen Sie die Nachricht zur Vorbelegung und bestimmen den verantwortlichen Personenkreis. Eine berechtigte Person erhält dann eine Mitarbeitendenaufgabe. Der Link zur Zahlung wird automatisch ergänzt.

                Optional legen Sie eigene Nachrichten für die Ansicht für Beteiligte nach bestätigter Zahlung oder bei Fehler und Abbruch fest. Bei vorheriger Prüfung können Sie eine Versandfrist festlegen. Eine Zahlungsfrist können Sie für beide Varianten einrichten.

                ### Was passiert bei der Ausführung?

                Prosuna berechnet die Zahlungspositionen und den Gesamtbetrag. Bei vorheriger Prüfung stehen diese Zahlungsdaten bereits fest: Die zuständige Person kann Betreff und Nachricht bearbeiten, die Positionen in dieser Aufgabe aber nur ansehen. Beim Versand legt Prosuna die Zahlung beim Zahlungsanbieter an und sendet die Zahlungsaufforderung über den Zustellweg der Identität.

                Danach wartet die Aufgabe auf die Zahlungsbestätigung. Beteiligte öffnen den Link und können zur Online-Zahlung beim Anbieter wechseln. Sobald die Zahlung bestätigt ist, setzt Prosuna den Vorgang über „Zahlung bestätigt“ fort. Schlägt die Zahlung fehl oder wird sie abgebrochen, schlägt die Aufgabe fehl.

                ### Welches Ergebnis steht danach bereit?

                Die Elementdaten enthalten unter anderem die empfangende Identität, den Zahlungslink, den Zahlungsanbieter, den Verwendungszweck, den Gesamtbetrag und die Zahlungsinformationen. Die Vorgangsdaten werden dadurch nicht automatisch geändert. Der Vorgangsverlauf zeigt den Versand und den Abschluss mit bestätigter Zahlung.

                Die Ansicht für Beteiligte zeigt den Zahlungsstatus und bietet nach bestätigter Zahlung eine Zahlungsbestätigung als PDF an.

                ### Wie sieht ein Beispiel aus?

                Sie richten zwei Zahlungspositionen mit 20,00 Euro und 5,00 Euro ein, jeweils mit Menge 1 und Steuersatz 0. Prosuna berechnet 25,00 Euro. Eine zuständige Person prüft die vorbereitete Nachricht und versendet die Zahlungsaufforderung. Nach bestätigter Online-Zahlung läuft der Vorgang weiter.

                ### Was ist zu beachten?

                Die berechneten Positionen müssen einen Gesamtbetrag über 0,00 Euro ergeben. Die Zahlungsdaten werden vor einer möglichen manuellen Nachrichtenbearbeitung berechnet.

                Die Zahlungsfrist beginnt mit dem Versand der Zahlungsaufforderung, nicht bereits mit der vorherigen Prüfung. Wird die Aufgabe wegen Fristüberschreitung beendet, läuft der Vorgang über den zugehörigen Fristausgang weiter. Eine spätere Zahlung führt diese beendete Aufgabe nicht nachträglich fort.

                Ist bei einem Neustart derselben Aufgabe bereits eine bestätigte Zahlung aus einem früheren Versuch vorhanden, übernimmt Prosuna diese und fordert keine neue Zahlung an. Prüfen Sie bei unklarem Zahlungsstand den bestehenden Zahlungsvorgang, bevor Sie eine neue Zahlungsaufforderung auslösen.
                """;
    }

    @Nonnull
    @Override
    public Class<PaymentRequestActionNodeConfig> getNodeConfigurationClass() {
        return PaymentRequestActionNodeConfig.class;
    }

    @Nonnull
    @Override
    @JsonIgnore
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper
                    .createFromPOJO(PaymentRequestActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerErrorWithDetails(e, "Die Einstellungen für „Zahlung anfordern“ konnten nicht geladen werden.", null);
        }

        layout.findChild(SemiAutomaticMessageConfig.GROUP_ID, GroupLayoutElement.class)
                .ifPresent(group -> SemiAutomaticMessageConfig.initConfigurationLayout(
                        group,
                        context.thisNode().getProcessId(),
                        context.thisNode().getProcessVersion()
                ));

        layout.findChild(SemiAutomaticMessageConfig.AutomaticContent.CONTENT_FIELD_ID, RichTextInputElement.class)
                .ifPresent(field -> field.setHint("Formulieren Sie die Zahlungsaufforderung. Angaben aus dem Vorgang können über Variablen eingefügt werden. Der Link zur Zahlung wird automatisch ergänzt."));
        layout.findChild(SemiAutomaticMessageConfig.ManualContent.CONTENT_FIELD_ID, RichTextInputElement.class)
                .ifPresent(field -> field.setHint("Diese Zahlungsaufforderung wird in der Mitarbeitendenaufgabe vorbereitet und kann dort geändert werden. Angaben aus dem Vorgang können über Variablen eingefügt werden. Der Link zur Zahlung wird automatisch ergänzt."));

        return layout;
    }

    @Nonnull
    @Override
    public List<ProcessNodeDeadlineDefinition> getDeadlines(@Nonnull PaymentRequestActionNodeConfig configuration) {
        return SemiAutomaticMessageConfig.isManual(configuration.messageConfig)
                ? List.of(MESSAGE_DISPATCH_DEADLINE, PAYMENT_DEADLINE) : List.of(PAYMENT_DEADLINE);
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts(@Nonnull PaymentRequestActionNodeConfig configuration) {
        return List.of(
                new ProcessNodePort(
                        PORT_PAID,
                        "Zahlung bestätigt",
                        "Der Vorgang wird fortgesetzt, nachdem der Zahlungsanbieter die Zahlung bestätigt hat."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_RECIPIENT_IDENTITY_ID,
                        "Empfangende Identität",
                        "Die ID der Identität, an die die Zahlungsaufforderung gesendet wurde.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_URL,
                        "Zahlungslink",
                        "Der Link zur Zahlungsaufgabe in der Ansicht für Beteiligte.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_PROVIDER_NAME,
                        "Zahlungsanbieter",
                        "Der Name des verwendeten Zahlungsanbieters.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_TRANSACTION_KEY,
                        "Transaktionsschlüssel",
                        "Der interne Schlüssel der Zahlungstransaktion.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_PURPOSE,
                        "Verwendungszweck",
                        "Der verwendete Verwendungszweck mit eingesetzten Angaben aus dem Vorgang.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_DESCRIPTION,
                        "Beschreibung",
                        "Die verwendete Zahlungsbeschreibung mit eingesetzten Angaben aus dem Vorgang.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_TOTAL,
                        "Gesamtbetrag (Euro)",
                        "Der berechnete Gesamtbetrag der Zahlungsanforderung in Euro.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_STATUS,
                        "Zahlungsstatus",
                        "Der bei dieser Aufgabe gespeicherte Zahlungsstatus.",
                        OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_DETAILS,
                        "Zahlungsdetails",
                        "Die bei dieser Aufgabe gespeicherten Zahlungsinformationen des Zahlungsanbieters. Nach bestätigter Zahlung werden sie aktualisiert.",
                        OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var paidAttempt = paymentTaskAttemptService.findPaidEarlierAttempt(context.getThisTask());
        if (paidAttempt.isPresent()) {
            return completeWithEarlierPayment(context, paidAttempt.get());
        }

        var configuration = context.getConfigurationOfExecutingNode();

        if (SemiAutomaticMessageConfig.isAutomatic(configuration.messageConfig)) {
            return initAutomatic(context, configuration);
        }
        if (SemiAutomaticMessageConfig.isManual(configuration.messageConfig)) {
            return initManual(context, configuration);
        }

        throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                "Wählen Sie unter „Nachrichtenversand“ aus, ob die Zahlungsaufforderung automatisch versendet oder vor dem Versand geprüft und bearbeitet werden soll."
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult completeWithEarlierPayment(
            @Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
            @Nonnull PaymentTaskAttemptService.PaidAttempt paidAttempt
    ) {
        var transaction = paidAttempt.transaction();
        // Keep the earlier request data, so outputs, the customer view and the payment confirmation stay available.
        var nodeData = new LinkedHashMap<>(paidAttempt.attempt().getNodeData());
        nodeData.put(OUTPUT_PAYMENT_STATUS, resolveStatus(transaction).getKey());
        nodeData.put(OUTPUT_PAYMENT_DETAILS, transaction.getPaymentInformation());
        paymentTaskAttemptService.logReuse(context.getLogger(), paidAttempt);

        return ProcessNodeExecutionResultTaskCompleted
                .of(PORT_PAID)
                .setRuntimeData(new LinkedHashMap<>(paidAttempt.attempt().getRuntimeData()))
                .setNodeData(nodeData)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    private ProcessNodeExecutionResult initAutomatic(
            @Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var resolvedConfiguration = resolveRequestConfiguration(
                configuration,
                context.getThisProcessInstance()
        );
        var automaticContent = requireAutomaticContent(configuration);
        var subject = automaticContent.subject.trim();
        var content = automaticContent.content.trim();
        var paymentPayload = createPaymentPayload(
                resolvedConfiguration.paymentConfig(),
                context.getCurrentProcessExecutionData()
        );

        return createPaymentRequest(
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                context.getThisTask(),
                resolvedConfiguration,
                paymentPayload,
                subject,
                content,
                null
        );
    }

    @Nonnull
    private ProcessNodeExecutionResult initManual(
            @Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionException {
        var resolvedConfiguration = resolveRequestConfiguration(configuration, context.getThisProcessInstance());
        var manualContent = requireManualContent(configuration);
        var paymentPayload = createPaymentPayload(
                resolvedConfiguration.paymentConfig(),
                context.getCurrentProcessExecutionData()
        );

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
                        "Im verantwortlichen Personenkreis wurde keine berechtigte Person gefunden, die die Zahlungsaufforderung bearbeiten kann."
                ));

        var runtimeData = new LinkedHashMap<>(context.getThisTask().getRuntimeData());
        runtimeData.put(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD, paymentPayload);

        return ProcessNodeExecutionResultTaskAssigned
                .of(assigneeUserId)
                .setNewDeadlineKey(MESSAGE_DISPATCH_DEADLINE.key())
                .setRuntimeData(runtimeData)
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeStaffView getStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context
    ) throws ResponseException {
        var paymentPayload = resolveRuntimePaymentPayloadForStaffView(context);

        var paymentInformation = new RichTextContentElement();
        paymentInformation.setId(STAFF_TASK_PAYMENT_INFORMATION_ID);
        paymentInformation.setContent(createPaymentInformationMarkdown(paymentPayload));

        var subjectField = new TextInputElement();
        subjectField.setId(STAFF_TASK_SUBJECT_FIELD_ID);
        subjectField.setLabel("Betreff der Zahlungsaufforderung");
        subjectField.setRequired(true);

        var contentField = new RichTextInputElement();
        contentField.setId(STAFF_TASK_CONTENT_FIELD_ID);
        contentField.setLabel("Nachricht");
        contentField.setHint("Prüfen und bearbeiten Sie die Zahlungsaufforderung vor dem Versand. Der Link zur Zahlung wird automatisch ergänzt.");
        contentField.setRequired(true);

        var root = new GroupLayoutElement();
        root.setId(STAFF_TASK_ROOT_ID);
        root.setChildren(new LinkedList<>(List.of(paymentInformation, subjectField, contentField)));

        var manualContent = requireManualContentForStaffView(context.getConfigurationOfExecutingNode());
        var taskViewData = new AuthoredElementValues();

        taskViewData.putLiteral(STAFF_TASK_SUBJECT_FIELD_ID, manualContent.subject.trim());
        taskViewData.putLiteral(STAFF_TASK_CONTENT_FIELD_ID, manualContent.content.trim());

        return ProcessNodeStaffView.of(
                context,
                root,
                List.of(new TaskViewEvent("Zahlungsaufforderung versenden", STAFF_TASK_SEND_EVENT)),
                taskViewData
        );
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
    @Override
    public Optional<ProcessNodeExecutionResult> onEventFromStaffTaskView(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context,
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
                    "Die Zahlungsaufforderung kann nur bei teilautomatisierter Ausführung über eine Mitarbeitendenaufgabe versendet werden."
            );
        }

        var subject = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_SUBJECT_FIELD_ID));
        var content = StringUtils.toNullableTrimmedString(update.getLiteral(STAFF_TASK_CONTENT_FIELD_ID));
        validateStaffMessage(subject, content);

        var resolvedConfiguration = resolveRequestConfiguration(
                configuration,
                context.getThisProcessInstance()
        );
        var paymentPayload = resolveRuntimePaymentPayload(context.getThisTask());
        var result = createPaymentRequest(
                context.getCurrentProcessExecutionData(),
                context.getThisProcessInstance(),
                context.getThisTask(),
                resolvedConfiguration,
                paymentPayload,
                subject,
                content,
                update
        );
        result.setClearCurrentlyAssignedUser(true);
        return Optional.of(result);
    }

    @Nullable
    @Override
    public ProcessNodeExecutionResult resume(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var transaction = resolveRuntimePaymentTransaction(context);
        var paymentStatus = resolveStatus(transaction);

        if (paymentStatus == XBezahldienstStatus.INITIAL) {
            return new ProcessNodeExecutionResultNoop();
        }

        if (paymentStatus != XBezahldienstStatus.PAYED) {
            throw new ProcessNodeExecutionExceptionIO(
                    paymentStatus == XBezahldienstStatus.CANCELED
                            ? "Die Zahlung wurde abgebrochen. Transaktionsschlüssel: %s."
                            : "Die Zahlung ist fehlgeschlagen. Transaktionsschlüssel: %s.",
                    StringUtils.quote(transaction.getKey())
            );
        }

        var nodeData = new LinkedHashMap<>(context.getThisTask().getNodeData());
        nodeData.put(OUTPUT_PAYMENT_STATUS, paymentStatus.getKey());
        nodeData.put(OUTPUT_PAYMENT_DETAILS, transaction.getPaymentInformation());

        return ProcessNodeExecutionResultTaskCompleted
                .of(PORT_PAID)
                .setRuntimeData(new LinkedHashMap<>(context.getThisTask().getRuntimeData()))
                .setNodeData(nodeData)
                .setProcessData(context.getThisTask().getProcessData());
    }

    @Nonnull
    @Override
    public ProcessNodeCustomerView getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) throws ResponseException {
        var paymentView = createPaymentView(context);
        return paymentView == null
                ? ProcessNodeDefinition.super.getCustomerTaskView(context)
                : paymentView;
    }

    @Nonnull
    @Override
    public ProcessNodeCustomerView getCompletedCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) throws ResponseException {
        var paymentView = createPaymentView(context);
        return paymentView == null
                ? ProcessNodeDefinition.super.getCompletedCustomerTaskView(context)
                : paymentView;
    }

    @Nullable
    private ProcessNodeCustomerView createPaymentView(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) throws ResponseException {
        var paymentTransactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (paymentTransactionKey == null) {
            return null;
        }

        var transaction = paymentTransactionService
                .retrieve(String.valueOf(paymentTransactionKey));

        if (transaction.isEmpty()) {
            return null;
        }

        var paymentPayloadRawData = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD);
        if (paymentPayloadRawData == null) {
            return null;
        }

        var paymentPayload = jsonMapper
                .convertValue(paymentPayloadRawData, PaymentPayload.class);

        var paymentProvider = paymentProviderRepository
                .findById(transaction.get().getPaymentProviderKey())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Zahlungsanbieter mit dem Schlüssel %s konnte nicht gefunden werden.".formatted(
                                StringUtils.quote(transaction.get().getPaymentProviderKey().toString())
                        )
                ));

        var paymentProviderDefinition = paymentProviderDefinitionsService
                .getProviderDefinition(
                        paymentProvider.getPaymentProviderDefinitionKey(),
                        paymentProvider.getPaymentProviderDefinitionVersion()
                )
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Die Definition des Zahlungsanbieters %s in Version %s konnte nicht gefunden werden.".formatted(
                                StringUtils.quote(paymentProvider.getPaymentProviderDefinitionKey()),
                                paymentProvider.getPaymentProviderDefinitionVersion()
                        )
                ));

        var paymentConfig = context.getConfigurationOfExecutingNode().payment;
        var successMessage = paymentConfig == null ? null : renderOptionalPaymentMessage(context, paymentConfig.successMessage());
        var failureMessage = paymentConfig == null ? null : renderOptionalPaymentMessage(context, paymentConfig.failureMessage());
        var downloadUrl = createPaymentConfirmationUrl(context);

        try {
            var layout = new PaymentGroupPreset(
                    paymentProvider,
                    paymentProviderDefinition,
                    paymentPayload,
                    transaction.get(),
                    successMessage,
                    failureMessage,
                    downloadUrl
            );
            return ProcessNodeCustomerView.of(context, layout, List.of(), new AuthoredElementValues());
        } catch (IOException | WriterException e) {
            throw ResponseException.internalServerErrorWithDetails(e, "Die Zahlungsansicht konnte nicht erstellt werden.", null);
        }
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(PaymentRequestActionNodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID);
        configuration.remove(PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.ManualContent.ASSIGNMENT_FIELD_ID);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_1);
        configuration.remove(SemiAutomaticMessageConfig.LayoutConfig.SIGNATURE_DEPARTMENT_FIELD_ID_2);
        return configuration;
    }

    @Nonnull
    private ResolvedRequestConfiguration resolveRequestConfiguration(
            @Nonnull PaymentRequestActionNodeConfig configuration,
            @Nonnull ProcessInstanceEntity processInstance
    ) throws ProcessNodeExecutionException {
        var paymentConfig = requirePaymentConfig(configuration);
        var recipientIdentityId = requireRecipientIdentity(configuration.recipientIdentityId);
        var recipientIdentity = processInstance.getIdentities().get(recipientIdentityId);
        if (recipientIdentity == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die ausgewählte empfangende Identität %s ist in diesem Vorgang nicht vorhanden.",
                    StringUtils.quote(recipientIdentityId)
            );
        }

        return new ResolvedRequestConfiguration(
                recipientIdentityId,
                paymentConfig,
                resolvePaymentProvider(paymentConfig),
                SemiAutomaticMessageConfig.resolveSignatureDepartment(
                        configuration.messageConfig,
                        vDepartmentShadowedService
                )
        );
    }

    @Nonnull
    private PaymentConfigElementValue requirePaymentConfig(@Nonnull PaymentRequestActionNodeConfig configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.payment == null || configuration.payment.paymentProviderKey() == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie einen Zahlungsanbieter für die Zahlungsanforderung aus.");
        }
        return configuration.payment;
    }

    @Nonnull
    private String requireRecipientIdentity(@Nullable String recipientIdentity) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var normalizedIdentity = StringUtils.toNullableTrimmedString(recipientIdentity);
        if (normalizedIdentity == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Wählen Sie die empfangende Identität für die Zahlungsaufforderung aus.");
        }
        return normalizedIdentity;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.AutomaticContent requireAutomaticContent(
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.automaticContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Legen Sie Betreff und Nachricht für den automatischen Versand fest."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContent(
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        var content = configuration.messageConfig == null ? null : configuration.messageConfig.manualContent;
        if (content == null
                || StringUtils.toNullableTrimmedString(content.subject) == null
                || StringUtils.toNullableTrimmedString(content.content) == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Legen Sie Betreff und Nachricht zur Vorbelegung der Mitarbeitendenaufgabe fest."
            );
        }
        return content;
    }

    @Nonnull
    private SemiAutomaticMessageConfig.ManualContent requireManualContentForStaffView(
            @Nonnull PaymentRequestActionNodeConfig configuration
    ) throws ResponseException {
        try {
            return requireManualContent(configuration);
        } catch (ProcessNodeExecutionExceptionInvalidConfiguration e) {
            throw ResponseException.internalServerError(e, e.getMessage());
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
                    new ComputedElementState().setError("Geben Sie einen Betreff für die Zahlungsaufforderung an.")
            );
        }
        if (content == null) {
            derivedRuntimeData.getElementStates().put(
                    STAFF_TASK_CONTENT_FIELD_ID,
                    new ComputedElementState().setError("Geben Sie eine Nachricht für die Zahlungsaufforderung an.")
            );
        }
        if (derivedRuntimeData.hasAnyError()) {
            throw ResponseException.badRequest(derivedRuntimeData);
        }
    }

    @Nonnull
    private PaymentProviderEntity resolvePaymentProvider(@Nonnull PaymentConfigElementValue paymentConfig) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        return paymentProviderRepository
                .findById(paymentConfig.paymentProviderKey())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Zahlungsanbieter mit dem Schlüssel %s wurde nicht gefunden.",
                        StringUtils.quote(paymentConfig.paymentProviderKey().toString())
                ));
    }

    @Nonnull
    private PaymentPayload createPaymentPayload(@Nonnull PaymentConfigElementValue paymentConfig,
                                                @Nonnull ProcessExecutionData processExecutionData) throws ProcessNodeExecutionException {
        Optional<PaymentPayload> paymentPayload;
        try {
            paymentPayload = paymentPayloadCreationService
                    .createRequest(paymentConfig, DerivedRuntimeElementData.empty(), processExecutionData);
        } catch (PaymentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die Zahlungsanforderung konnte nicht vorbereitet werden: %s", e.getMessage());
        }

        return paymentPayload
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Zahlungsanforderung enthält keine zahlbaren Positionen oder ergibt einen Gesamtbetrag von 0,00 Euro."
                ));
    }

    @Nonnull
    private PaymentTransactionEntity createPaymentTransaction(@Nonnull PaymentProviderEntity paymentProvider,
                                                              @Nonnull PaymentPayload paymentPayload,
                                                              @Nonnull String paymentUrl) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return paymentTransactionService
                    .create(paymentProvider, paymentPayload, paymentUrl);
        } catch (PaymentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die Zahlung konnte beim Zahlungsanbieter nicht angelegt werden.");
        }
    }

    @Nonnull
    private ProcessNodeExecutionResult createPaymentRequest(
            @Nonnull ProcessExecutionData processExecutionData,
            @Nonnull ProcessInstanceEntity processInstance,
            @Nonnull ProcessInstanceTaskEntity task,
            @Nonnull ResolvedRequestConfiguration resolvedConfiguration,
            @Nonnull PaymentPayload paymentPayload,
            @Nonnull String subject,
            @Nonnull String content,
            @Nullable AuthoredElementValues staffTaskViewData
    ) throws ProcessNodeExecutionException {
        var paymentUrl = createPaymentUrl(processInstance, task);
        var transaction = createPaymentTransaction(
                resolvedConfiguration.paymentProvider(),
                paymentPayload,
                paymentUrl
        );

        var runtimeData = new LinkedHashMap<>(task.getRuntimeData());
        runtimeData.put(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD, paymentPayload);
        runtimeData.put(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, transaction.getKey());
        if (staffTaskViewData != null) {
            runtimeData.put(STAFF_TASK_VIEW_DATA_RUNTIME_KEY, staffTaskViewData.clone());
        }

        return new ProcessNodeExecutionResultPaymentRequested(
                transaction.getKey(),
                resolvedConfiguration.paymentProvider().getName()
        )
                .setNewDeadlineKey(PAYMENT_DEADLINE.key())
                .setRuntimeData(runtimeData)
                .setNodeData(createNodeData(
                        resolvedConfiguration.recipientIdentityId(),
                        paymentUrl,
                        resolvedConfiguration.paymentProvider(),
                        paymentPayload,
                        transaction
                ))
                .setProcessData(processExecutionData.getProcessData())
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest(
                        resolvedConfiguration.recipientIdentityId(),
                        new CommunicationMessage.Builder()
                                .subject(subject)
                                .body(MarkdownRenderer.renderPlainText(content))
                                .markdownBody(content)
                                .callToActions(List.of(new CommunicationMessageCallToAction(
                                        "Zahlung durchführen",
                                        paymentUrl
                                )))
                                .signatureDepartment(resolvedConfiguration.signatureDepartment())
                                .build(),
                        null
                ));
    }

    @Nonnull
    private PaymentPayload resolveRuntimePaymentPayload(
            @Nonnull ProcessInstanceTaskEntity task
    ) throws ProcessNodeExecutionException {
        var paymentPayloadData = task
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD);
        if (paymentPayloadData == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Für diese Zahlungsaufforderung liegen keine vorbereiteten Zahlungsdaten vor."
            );
        }
        if (paymentPayloadData instanceof PaymentPayload paymentPayload) {
            return paymentPayload;
        }

        try {
            return jsonMapper.convertValue(paymentPayloadData, PaymentPayload.class);
        } catch (RuntimeException e) {
            // The two-argument overload drops the cause; use the cause-preserving formatted overload.
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "%s",
                    "Die vorbereiteten Zahlungsdaten konnten nicht gelesen werden."
            );
        }
    }

    @Nonnull
    private PaymentPayload resolveRuntimePaymentPayloadForStaffView(
            @Nonnull ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeConfig> context
    ) throws ResponseException {
        try {
            return resolveRuntimePaymentPayload(context.getThisTask());
        } catch (ProcessNodeExecutionException e) {
            throw ResponseException.internalServerError(e, e.getMessage());
        }
    }

    @Nonnull
    private static String createPaymentInformationMarkdown(@Nonnull PaymentPayload paymentPayload) {
        // Purpose and descriptions can come from process data, including customer input, so they are shown as typed.
        var itemMarkdown = paymentPayload
                .getPaymentItems()
                .stream()
                .map(item -> "- %s: %s Euro%s".formatted(
                        MarkdownTemplateValues.escapeText(item.getDescription()),
                        NumberUtils.formatGermanNumber(item.getTotalPrice(), 2),
                        item.getTaxRate().signum() > 0
                                ? " inkl. %s %% Steuern".formatted(formatTaxRate(item.getTaxRate()))
                                : ""
                ))
                .collect(Collectors.joining("\n"));
        var includesTaxes = paymentPayload
                .getPaymentItems()
                .stream()
                .anyMatch(item -> item.getTaxRate().signum() > 0);

        return """
                # Zahlungsinformationen

                **Verwendungszweck:** %s

                **Beschreibung:** %s

                **Zahlungspositionen:**

                %s

                **Gesamtbetrag:** %s Euro%s
                """.formatted(
                MarkdownTemplateValues.escapeText(paymentPayload.getPurpose()),
                MarkdownTemplateValues.escapeText(paymentPayload.getDescription()),
                itemMarkdown,
                NumberUtils.formatGermanNumber(paymentPayload.getTotal(), 2),
                includesTaxes ? " inkl. Steuern" : ""
        );
    }

    // Show every configured tax rate decimal so staff see the rate that was used for the calculation.
    @Nonnull
    private static String formatTaxRate(@Nonnull BigDecimal taxRate) {
        return NumberUtils.formatGermanNumber(taxRate, 2, PaymentItem.TAX_RATE_SCALE);
    }

    @Nonnull
    private PaymentTransactionEntity resolveRuntimePaymentTransaction(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionExceptionMissingValue {
        var transactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (transactionKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für diese Zahlungsaufforderung ist keine Zahlung hinterlegt.");
        }

        return paymentTransactionService
                .retrieve(String.valueOf(transactionKey))
                .orElseThrow(() -> new ProcessNodeExecutionExceptionMissingValue(
                        "Die hinterlegte Zahlung konnte nicht gefunden werden. Transaktionsschlüssel: %s.",
                        StringUtils.quote(String.valueOf(transactionKey))
                ));
    }

    @Nonnull
    private LinkedHashMap<String, Object> createNodeData(@Nonnull String recipientIdentityId,
                                                         @Nonnull String paymentUrl,
                                                         @Nonnull PaymentProviderEntity paymentProvider,
                                                         @Nonnull PaymentPayload paymentPayload,
                                                         @Nonnull PaymentTransactionEntity transaction) {
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_RECIPIENT_IDENTITY_ID, recipientIdentityId);
        nodeData.put(OUTPUT_PAYMENT_URL, paymentUrl);
        nodeData.put(OUTPUT_PAYMENT_PROVIDER_NAME, paymentProvider.getName());
        nodeData.put(OUTPUT_PAYMENT_TRANSACTION_KEY, transaction.getKey());
        nodeData.put(OUTPUT_PAYMENT_PURPOSE, paymentPayload.getPurpose());
        nodeData.put(OUTPUT_PAYMENT_DESCRIPTION, paymentPayload.getDescription());
        nodeData.put(OUTPUT_PAYMENT_TOTAL, paymentPayload.getTotal());
        nodeData.put(OUTPUT_PAYMENT_STATUS, resolveStatus(transaction).getKey());
        nodeData.put(OUTPUT_PAYMENT_DETAILS, transaction.getPaymentInformation());
        return nodeData;
    }

    @Nonnull
    private String createPaymentUrl(@Nonnull ProcessInstanceEntity processInstance,
                                    @Nonnull ProcessInstanceTaskEntity task) {
        return prosunaConfig.createUrl(
                "/process/",
                processInstance.getAccessKey(),
                "tasks",
                task.getAccessKey()
        );
    }

    @Nonnull
    private String createPaymentConfirmationUrl(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) {
        return prosunaConfig.createUrlWithTrailingSlash(
                "/api/public/processes/",
                context.getThisProcessInstance().getAccessKey(),
                "tasks",
                context.getThisTask().getAccessKey(),
                "payment-confirmation"
        );
    }

    @Nullable
    private String renderOptionalPaymentMessage(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context,
                                                @Nullable String template) throws ResponseException {
        if (StringUtils.isNullOrEmpty(template)) {
            return null;
        }

        var processExecutionData = new ProcessExecutionData()
                .addProcessData(context.getThisTask().getProcessData());

        try {
            // The message is shown as Markdown on the payment status page.
            return templateRenderService.interpolate(processExecutionData, template, TemplateOutputFormat.Markdown);
        } catch (RuntimeException e) {
            throw ResponseException.internalServerErrorWithDetails(
                    e,
                    "Die Nachricht zur Zahlung konnte nicht vorbereitet werden.",
                    null
            );
        }
    }

    @Nonnull
    private static XBezahldienstStatus resolveStatus(@Nonnull PaymentTransactionEntity transaction) {
        var status = transaction.getStatus();
        return status == null ? XBezahldienstStatus.INITIAL : status;
    }

    private record ResolvedRequestConfiguration(
            @Nonnull String recipientIdentityId,
            @Nonnull PaymentConfigElementValue paymentConfig,
            @Nonnull PaymentProviderEntity paymentProvider,
            @Nullable VDepartmentShadowedEntity signatureDepartment
    ) {
    }

    /**
     * Configuration for creating a payable customer task and sending its payment request to one process identity.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class PaymentRequestActionNodeConfig {
        public static final String RECIPIENT_IDENTITY_ID_FIELD_ID = "recipientIdentityId";
        public static final String PAYMENT_FIELD_ID = "payment";

        /**
         * Logical process identity receiving the payment request. A missing identity or an identity that is not
         * present in the process instance prevents initialization or dispatch. Every configured provider option
         * must have a usable communication binding because the request is sent through the selected identity.
         */
        @InputElementPOJOBinding(id = RECIPIENT_IDENTITY_ID_FIELD_ID, type = ElementType.ProcessIdentityIdInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Empfangende Identität"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Wählen Sie die Identität des Vorgangs aus, an die die Zahlungsaufforderung versendet werden soll. Prosuna verwendet deren hinterlegten Zustellweg."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true),
                @ElementPOJOBindingProperty(key = "requiresCommunication", boolValue = true)
        })
        public String recipientIdentityId;

        /**
         * Payment provider, purpose, description, requestor mapping and payable items used to create the transaction.
         */
        @InputElementPOJOBinding(id = PAYMENT_FIELD_ID, type = ElementType.PaymentConfig, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zahlung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Legen Sie den Zahlungsanbieter, die Zahlungspositionen und die Meldungen für Beteiligte fest."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public PaymentConfigElementValue payment;

        /**
         * Dispatch mode, message templates and optional staff assignment.
         */
        public SemiAutomaticMessageConfig.LayoutConfig messageConfig;
    }
}
