package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.zxing.WriterException;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.prosuna.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.prosuna.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.prosuna.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.prosuna.backend.elements.utils.ElementPOJOMapper;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.payment.models.PaymentItem;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.plugins.core.CorePlugin;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessNodeType;
import de.aivot.prosuna.backend.process.exceptions.*;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeOutput;
import de.aivot.prosuna.backend.process.models.ProcessNodePort;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultNoop;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.utils.NumberUtils;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PaymentRequestActionNodeV1 implements ProcessNodeDefinition<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> {
    public static final String NODE_KEY = "payment_request";

    private static final String PORT_PAID = "paid";

    private static final String OUTPUT_RECIPIENT_EMAIL = "recipientEmail";
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

    private final PaymentPayloadCreationService paymentPayloadCreationService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private final TemplateRenderService templateRenderService;
    private final ProsunaConfig prosunaConfig;
    private final MailService mailService;
    private final ProcessService processService;
    private final DepartmentService departmentService;
    private final JsonMapper jsonMapper;

    public PaymentRequestActionNodeV1(PaymentPayloadCreationService paymentPayloadCreationService,
                                      PaymentTransactionService paymentTransactionService,
                                      PaymentProviderRepository paymentProviderRepository,
                                      PaymentProviderDefinitionsService paymentProviderDefinitionsService,
                                      TemplateRenderService templateRenderService,
                                      ProsunaConfig prosunaConfig,
                                      MailService mailService,
                                      ProcessService processService,
                                      DepartmentService departmentService, JsonMapper jsonMapper) {
        this.paymentPayloadCreationService = paymentPayloadCreationService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentProviderDefinitionsService = paymentProviderDefinitionsService;
        this.templateRenderService = templateRenderService;
        this.prosunaConfig = prosunaConfig;
        this.mailService = mailService;
        this.processService = processService;
        this.departmentService = departmentService;
        this.jsonMapper = jsonMapper;
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
        return new ProcessNodeExecutionType[]{ProcessNodeExecutionType.SemiAutomatic};
    }

    @Nonnull
    @Override
    public String getName() {
        return "Zahlung anfordern";
    }

    @Nonnull
    @Override
    public String getAbstract() {
        return "Fordert eine Online-Zahlung an und wartet auf deren erfolgreichen Abschluss.";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Erstellt eine Zahlungstransaktion, sendet Zahlungsinformationen per E-Mail und wartet auf die erfolgreiche Zahlung.";
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
        try {
            return ElementPOJOMapper
                    .createFromPOJO(PaymentRequestActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(e, "Fehler bei der Erstellung des Konfigurationslayouts: %s", e.getMessage());
        }
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(
                new ProcessNodePort(
                        PORT_PAID,
                        "Zahlung erfolgreich",
                        "Der Prozess wird fortgesetzt, nachdem die Zahlung erfolgreich abgeschlossen wurde."
                )
        );
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        OUTPUT_RECIPIENT_EMAIL,
                        "E-Mail-Adresse",
                        "Die Empfängeradresse, an die die Zahlungsinformationen gesendet wurden.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_URL,
                        "Zahlungslink",
                        "Der öffentliche Link zur Zahlungsaufgabe.",
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
                        "Buchungszweck",
                        "Der gerenderte Buchungszweck der Zahlungsanforderung.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_DESCRIPTION,
                        "Beschreibung",
                        "Die gerenderte Beschreibung der Zahlungsanforderung.",
                        "string"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_TOTAL,
                        "Gesamtbetrag",
                        "Der Gesamtbetrag der Zahlungsanforderung.",
                        "number"
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_STATUS,
                        "Zahlungsstatus",
                        "Der Status der Zahlungstransaktion.",
                        OUTPUT_PAYMENT_STATUS_TYPE_DEFINITION
                ),
                new ProcessNodeOutput(
                        OUTPUT_PAYMENT_DETAILS,
                        "Zahlungsdetails",
                        "Die Zahlungsinformationen des Zahlungsanbieters nach Abschluss der Zahlung.",
                        OUTPUT_PAYMENT_DETAILS_TYPE_DEFINITION
                )
        );
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var configuration = context.getConfigurationOfExecutingNode();
        var paymentConfig = requirePaymentConfig(configuration);
        var recipientEmail = resolveRecipientEmail(context.getCurrentProcessExecutionData(), configuration.recipientEmail);
        var paymentProvider = resolvePaymentProvider(paymentConfig);
        var paymentPayload = createPaymentPayload(paymentConfig, context.getCurrentProcessExecutionData());
        var paymentUrl = createPaymentUrl(context);
        var process = resolveProcess(context);
        var department = tryResolveDepartment(process);
        var transaction = createPaymentTransaction(paymentProvider, paymentPayload, paymentUrl);

        sendPaymentRequestMail(
                context,
                process,
                department,
                paymentProvider,
                paymentPayload,
                recipientEmail,
                paymentUrl
        );

        return new ProcessNodeExecutionResultPaymentRequested(transaction.getKey(), paymentProvider.getName())
                .setRuntimeData(Map.of(
                        PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD, paymentPayload,
                        PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, transaction.getKey()
                ))
                .setNodeData(createNodeData(recipientEmail, paymentUrl, paymentProvider, paymentPayload, transaction))
                .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
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
                    "Die Zahlungstransaktion %s wurde nicht erfolgreich abgeschlossen. Status: %s%s",
                    StringUtils.quote(transaction.getKey()),
                    StringUtils.quote(paymentStatus.getKey()),
                    StringUtils.isNotNullOrEmpty(transaction.getPaymentError()) ? " Fehler: " + transaction.getPaymentError() : ""
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
    public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer<PaymentRequestActionNodeConfig> context) throws ResponseException {
        var paymentTransactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (paymentTransactionKey == null) {
            return ProcessNodeDefinition.super.getCustomerTaskView(context);
        }

        var transaction = paymentTransactionService
                .retrieve(String.valueOf(paymentTransactionKey));

        if (transaction.isEmpty()) {
            return ProcessNodeDefinition.super.getCustomerTaskView(context);
        }

        var paymentPayloadRawData = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD);
        if (paymentPayloadRawData == null) {
            return ProcessNodeDefinition.super.getCustomerTaskView(context);
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
            return new PaymentGroupPreset(
                    paymentProvider,
                    paymentProviderDefinition,
                    paymentPayload,
                    transaction.get(),
                    successMessage,
                    failureMessage,
                    downloadUrl
            );
        } catch (IOException | WriterException e) {
            throw ResponseException.internalServerError(e);
        }
    }

    @Nonnull
    @Override
    public AuthoredElementValues cleanConfigurationForExport(@Nonnull AuthoredElementValues configuration) {
        configuration.remove(PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID);
        return configuration;
    }

    @Nonnull
    private PaymentConfigElementValue requirePaymentConfig(@Nonnull PaymentRequestActionNodeConfig configuration) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (configuration.payment == null || configuration.payment.paymentProviderKey() == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die Zahlungsanforderung muss ein Zahlungsanbieter konfiguriert sein.");
        }
        return configuration.payment;
    }

    @Nonnull
    private String resolveRecipientEmail(@Nonnull ProcessExecutionData processExecutionData,
                                         @Nullable String recipientTemplate) throws ProcessNodeExecutionException {
        if (StringUtils.isNullOrEmpty(recipientTemplate)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die Zahlungsanforderung muss eine E-Mail-Adresse angegeben werden.");
        }

        var recipientEmail = templateRenderService
                .interpolate(processExecutionData, recipientTemplate)
                .trim();

        if (StringUtils.isNullOrEmpty(recipientEmail)) {
            throw new ProcessNodeExecutionExceptionMissingValue("Die E-Mail-Adresse für die Zahlungsanforderung ist nach der Verarbeitung leer.");
        }

        try {
            var recipients = InternetAddress.parse(recipientEmail, true);
            if (recipients.length != 1) {
                throw new ProcessNodeExecutionExceptionInvalidConfiguration("Für die Zahlungsanforderung darf genau eine E-Mail-Adresse angegeben werden.");
            }
            return recipients[0].getAddress();
        } catch (AddressException e) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(e, "Die E-Mail-Adresse %s ist ungültig.", StringUtils.quote(recipientEmail));
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
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die Zahlungsanforderung konnte nicht erstellt werden: %s", e.getMessage());
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
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die Zahlungstransaktion konnte nicht erstellt werden: %s", e.getMessage());
        }
    }

    @Nonnull
    private ProcessEntity resolveProcess(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionExceptionUnknown {
        try {
            return processService
                    .retrieve(context.getThisProcessInstance().getProcessId())
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                            "Der Prozess mit der ID %d wurde nicht gefunden.",
                            context.getThisProcessInstance().getProcessId()
                    ));
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Der Prozess mit der ID %d konnte nicht geladen werden.", context.getThisProcessInstance().getProcessId());
        }
    }

    private void sendPaymentRequestMail(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
                                        @Nonnull ProcessEntity process,
                                        @Nonnull DepartmentEntity department,
                                        @Nonnull PaymentProviderEntity paymentProvider,
                                        @Nonnull PaymentPayload paymentPayload,
                                        @Nonnull String recipientEmail,
                                        @Nonnull String paymentUrl) throws ProcessNodeExecutionException {
        var subject = "[Prosuna] "
                + (context.getThisProcessInstance().getCreatedForTestClaimId() != null ? "[Test] " : "")
                + "Zahlungsaufforderung";

        var mailData = new LinkedHashMap<String, Object>();
        mailData.put("process", process);
        mailData.put("processInstance", context.getThisProcessInstance());
        mailData.put("processInstanceTask", context.getThisTask());
        mailData.put("taskName", context.getThisNode().resolveName(this));
        mailData.put("paymentProvider", paymentProvider);
        mailData.put("paymentPayload", paymentPayload);
        mailData.put("paymentItems", paymentPayload.getPaymentItems());
        mailData.put("paymentItemLines", formatPaymentItemLines(paymentPayload));
        mailData.put("paymentTotalLabel", formatMoney(paymentPayload.getTotal()));
        mailData.put("paymentUrl", paymentUrl);

        try {
            mailService.sendMail(
                    departmentService.getDepartmentTheme(department),
                    recipientEmail,
                    Optional.empty(),
                    Optional.empty(),
                    subject,
                    MailTemplate.ProcessPaymentRequested,
                    mailData,
                    Optional.empty()
            );
        } catch (MessagingException | MailException | IOException | ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(e, "Die E-Mail mit der Zahlungsanforderung konnte nicht versendet werden: %s", e.getMessage());
        }
    }

    @Nonnull
    private DepartmentEntity tryResolveDepartment(@Nonnull ProcessEntity process) throws ProcessNodeExecutionExceptionUnknown {
        return departmentService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionUnknown(
                        "Die Organisationseinheit mit der ID %d wurde nicht gefunden.",
                        process.getDepartmentId()
                ));
    }

    @Nonnull
    private PaymentTransactionEntity resolveRuntimePaymentTransaction(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionExceptionMissingValue {
        var transactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY);

        if (transactionKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue("Für die Zahlungsanforderung wurde keine Zahlungstransaktion in den Laufzeitdaten gefunden.");
        }

        return paymentTransactionService
                .retrieve(String.valueOf(transactionKey))
                .orElseThrow(() -> new ProcessNodeExecutionExceptionMissingValue(
                        "Die Zahlungstransaktion %s wurde nicht gefunden.",
                        StringUtils.quote(String.valueOf(transactionKey))
                ));
    }

    @Nonnull
    private LinkedHashMap<String, Object> createNodeData(@Nonnull String recipientEmail,
                                                         @Nonnull String paymentUrl,
                                                         @Nonnull PaymentProviderEntity paymentProvider,
                                                         @Nonnull PaymentPayload paymentPayload,
                                                         @Nonnull PaymentTransactionEntity transaction) {
        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(OUTPUT_RECIPIENT_EMAIL, recipientEmail);
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
    private String createPaymentUrl(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) {
        return prosunaConfig.createUrl(
                "/process/",
                context.getThisProcessInstance().getAccessKey(),
                "tasks",
                context.getThisTask().getAccessKey()
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
            return templateRenderService.interpolate(processExecutionData, template);
        } catch (RuntimeException e) {
            throw ResponseException.internalServerError(
                    e,
                    "Die Vorlage %s konnte nicht gerendert werden.",
                    StringUtils.quote(template)
            );
        }
    }

    @Nonnull
    private static XBezahldienstStatus resolveStatus(@Nonnull PaymentTransactionEntity transaction) {
        var status = transaction.getStatus();
        return status == null ? XBezahldienstStatus.INITIAL : status;
    }

    @Nonnull
    private static String formatMoney(@Nonnull BigDecimal value) {
        return NumberUtils.formatGermanNumber(value, 2) + " Euro";
    }

    @Nonnull
    private static List<String> formatPaymentItemLines(@Nonnull PaymentPayload paymentPayload) {
        var paymentItems = paymentPayload.getPaymentItems();
        if (paymentItems == null) {
            return List.of();
        }

        return paymentItems
                .stream()
                .map(PaymentRequestActionNodeV1::formatPaymentItemLine)
                .toList();
    }

    @Nonnull
    private static String formatPaymentItemLine(@Nonnull PaymentItem item) {
        var taxInfo = item.getTaxRate() != null && item.getTaxRate().compareTo(BigDecimal.ZERO) > 0
                ? " inkl. " + NumberUtils.formatGermanNumber(item.getTaxRate(), 2) + " % Steuern"
                : "";
        return "%s: %s%s".formatted(item.getDescription(), formatMoney(item.getTotalPrice()), taxInfo);
    }

    /**
     * Configuration for creating a payable customer task and sending the payment link to one recipient.
     */
    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class PaymentRequestActionNodeConfig {
        public static final String PAYMENT_FIELD_ID = "payment";
        public static final String RECIPIENT_EMAIL_FIELD_ID = "recipientEmail";

        /**
         * Payment provider, purpose, description, requestor mapping and payable items used to create the transaction.
         */
        @InputElementPOJOBinding(id = PAYMENT_FIELD_ID, type = ElementType.PaymentConfig, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zahlung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Konfiguration der Zahlungsanforderung."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public PaymentConfigElementValue payment;

        /**
         * Recipient email template rendered with the current process data. Exactly one resolved email address is allowed.
         */
        @InputElementPOJOBinding(id = RECIPIENT_EMAIL_FIELD_ID, type = ElementType.Text, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "E-Mail-Adresse"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Empfänger:in der Zahlungsinformationen. Unterstützt Vorlagen mit Vorgangsdaten."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String recipientEmail;
    }
}
