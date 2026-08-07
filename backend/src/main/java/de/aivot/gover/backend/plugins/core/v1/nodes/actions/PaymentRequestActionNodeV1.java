package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import com.google.zxing.WriterException;
import de.aivot.gover.backend.core.services.ObjectMapperFactory;
import de.aivot.gover.backend.department.services.DepartmentService;
import de.aivot.gover.backend.elements.annotations.ElementPOJOBindingProperty;
import de.aivot.gover.backend.elements.annotations.InputElementPOJOBinding;
import de.aivot.gover.backend.elements.annotations.LayoutElementPOJOBinding;
import de.aivot.gover.backend.elements.exceptions.ElementDataConversionException;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.gover.backend.elements.models.elements.layout.ConfigLayoutElement;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.gover.backend.elements.utils.ElementPOJOMapper;
import de.aivot.gover.backend.enums.ElementType;
import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.mail.enums.MailTemplate;
import de.aivot.gover.backend.mail.services.MailService;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.exceptions.PaymentException;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import de.aivot.gover.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.gover.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.gover.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.gover.backend.payment.services.PaymentTransactionService;
import de.aivot.gover.backend.plugins.core.CorePlugin;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.enums.ProcessNodeExecutionLogLevel;
import de.aivot.gover.backend.process.enums.ProcessNodeType;
import de.aivot.gover.backend.process.exceptions.*;
import de.aivot.gover.backend.process.models.*;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResult;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.services.ProcessService;
import de.aivot.gover.backend.theme.entities.ThemeEntity;
import de.aivot.gover.backend.utils.NumberUtils;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class PaymentRequestActionNodeV1 implements ProcessNodeDefinition<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> {
    public static final String NODE_KEY = "payment-request";

    private static final String PORT_NAME = "output";

    public static final String DATA_KEY_RECIPIENT_EMAIL = "recipientEmail";
    public static final String DATA_KEY_PAYMENT_PAYLOAD = PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD;
    public static final String DATA_KEY_PAYMENT_TRANSACTION = PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION;
    public static final String DATA_KEY_PAYMENT_DETAILS = "paymentDetails";

    private final GoverConfig goverConfig;
    private final PaymentPayloadCreationService paymentPayloadCreationService;
    private final PaymentTransactionService paymentTransactionService;
    private final PaymentProviderRepository paymentProviderRepository;
    private final MailService mailService;
    private final ProcessService processService;
    private final DepartmentService departmentService;

    public PaymentRequestActionNodeV1(GoverConfig goverConfig,
                                      PaymentPayloadCreationService paymentPayloadCreationService,
                                      PaymentTransactionService paymentTransactionService,
                                      PaymentProviderRepository paymentProviderRepository,
                                      MailService mailService,
                                      ProcessService processService,
                                      DepartmentService departmentService) {
        this.goverConfig = goverConfig;
        this.paymentPayloadCreationService = paymentPayloadCreationService;
        this.paymentTransactionService = paymentTransactionService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.mailService = mailService;
        this.processService = processService;
        this.departmentService = departmentService;
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
        return "Zahlung anfordern";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Fordert eine Zahlung per E-Mail an und wartet auf den Zahlungsabschluss.";
    }

    @Nonnull
    @Override
    public ProcessNodeType getType() {
        return ProcessNodeType.Action;
    }

    @Nonnull
    @Override
    public List<ProcessNodePort> getPorts() {
        return List.of(new ProcessNodePort(
                PORT_NAME,
                "Zahlung abgeschlossen",
                "Der Prozess wird hier fortgesetzt, nachdem die Zahlung erfolgreich abgeschlossen wurde."
        ));
    }

    @Nonnull
    @Override
    public List<ProcessNodeOutput> getOutputs() {
        return List.of(
                new ProcessNodeOutput(
                        DATA_KEY_RECIPIENT_EMAIL,
                        "Empfänger:in",
                        "Die E-Mail-Adresse, an welche die Zahlungsanforderung gesendet wurde."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_PAYMENT_TRANSACTION,
                        "Zahlungstransaktion",
                        "Der Schlüssel der erstellten Zahlungstransaktion."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_PAYMENT_PAYLOAD,
                        "Zahlungsanforderung",
                        "Die Zahlungsanforderung mit Positionen und Gesamtbetrag."
                ),
                new ProcessNodeOutput(
                        DATA_KEY_PAYMENT_DETAILS,
                        "Zahlungsdetails",
                        "Die Zahlungsdetails der erfolgreich abgeschlossenen Zahlung."
                )
        );
    }

    @Nonnull
    @Override
    public ConfigLayoutElement getConfigurationLayout(@Nonnull ProcessNodeDefinitionConfigurationLayoutContext context) throws ResponseException {
        ConfigLayoutElement layout;
        try {
            layout = ElementPOJOMapper.createFromPOJO(PaymentRequestActionNodeConfig.class);
        } catch (ElementDataConversionException e) {
            throw ResponseException.internalServerError(
                    "Fehler beim Erstellen des Konfigurations-Layouts für die Zahlungsanforderung: %s",
                    e.getMessage()
            );
        }

        layout
                .findChild(PaymentRequestActionNodeConfig.RECIPIENT_EMAIL_PROCESS_DATA_KEY_FIELD_ID, ProcessDataKeyInputElement.class)
                .ifPresent(field -> field.setDisableWildCards(true));

        return layout;
    }

    @Override
    public ProcessNodeExecutionResult init(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var config = context.getConfigurationOfExecutingNode();
        var paymentConfig = requirePaymentConfig(config);
        var recipientEmail = resolveRecipientEmail(context, config);

        var paymentProvider = paymentProviderRepository
                .findById(paymentConfig.paymentProviderKey())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Der Zahlungsanbieter mit dem Schlüssel %s konnte nicht gefunden werden.",
                        StringUtils.quote(paymentConfig.paymentProviderKey().toString())
                ));

        Optional<PaymentPayload> paymentPayload;
        try {
            paymentPayload = paymentPayloadCreationService.createRequest(
                    paymentConfig,
                    new DerivedRuntimeElementData(),
                    context.getCurrentProcessExecutionData()
            );
        } catch (PaymentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Erstellen der Zahlungsanforderung: %s",
                    e.getMessage()
            );
        }

        if (paymentPayload.isEmpty()) {
            context
                    .getLogger()
                    .logf(
                            ProcessNodeExecutionLogLevel.Info,
                            false,
                            true,
                            "Keine Zahlungsanforderung erstellt",
                            "Es wurde keine Zahlungsanforderung erstellt, da keine Zahlungspositionen mit einem Gesamtwert größer als 0 gefunden wurden."
                    );

            return ProcessNodeExecutionResultTaskCompleted
                    .of(PORT_NAME)
                    .setProcessData(context.getCurrentProcessExecutionData().getProcessData());
        }

        var customerTaskUrl = createCustomerTaskUrl(context);

        PaymentTransactionEntity transaction;
        try {
            transaction = paymentTransactionService.create(
                    paymentProvider,
                    paymentPayload.get(),
                    customerTaskUrl
            );
        } catch (PaymentException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Fehler beim Absenden der Zahlungsanforderung: %s",
                    e.getMessage()
            );
        }

        sendPaymentRequestMail(
                context,
                recipientEmail,
                paymentProvider.getName(),
                paymentPayload.get(),
                customerTaskUrl
        );

        return new ProcessNodeExecutionResultPaymentRequested(
                transaction.getKey(),
                paymentProvider.getName()
        ).setRuntimeData(Map.of(
                DATA_KEY_PAYMENT_PAYLOAD, paymentPayload.get(),
                DATA_KEY_PAYMENT_TRANSACTION, transaction.getKey(),
                DATA_KEY_RECIPIENT_EMAIL, recipientEmail
        ));
    }

    @Nullable
    @Override
    public ProcessNodeExecutionResult resume(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) throws ProcessNodeExecutionException {
        var txKey = StringUtils.toNullableTrimmedString(
                context.getThisTask().getRuntimeData().get(DATA_KEY_PAYMENT_TRANSACTION)
        );
        if (txKey == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Zahlungstransaktion für die Zahlungsanforderung fehlt."
            );
        }

        var tx = paymentTransactionService
                .retrieve(txKey)
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Zahlungsanforderung mit dem Schlüssel %s konnte nicht gefunden werden.",
                        StringUtils.quote(txKey)
                ));

        if (tx.getStatus() != XBezahldienstStatus.PAYED) {
            if (StringUtils.isNotNullOrEmpty(tx.getPaymentError())) {
                throw new ProcessNodeExecutionExceptionIO(
                        "Die Zahlungsanforderung mit dem Schlüssel %s ist nicht abgeschlossen. Aktueller Status: %s. Der folgende Fehler wurde gemeldet: %s",
                        StringUtils.quote(txKey),
                        StringUtils.quote(tx.getStatus().getKey()),
                        StringUtils.quote(tx.getPaymentError())
                );
            }
            throw new ProcessNodeExecutionExceptionIO(
                    "Die Zahlungsanforderung mit dem Schlüssel %s ist nicht abgeschlossen. Aktueller Status: %s.",
                    StringUtils.quote(txKey),
                    StringUtils.quote(tx.getStatus().getKey())
            );
        }

        context
                .getLogger()
                .logf(
                        ProcessNodeExecutionLogLevel.Info,
                        false,
                        true,
                        "Zahlungsanforderung abgeschlossen",
                        "Die Zahlungsanforderung mit dem Schlüssel %s wurde erfolgreich abgeschlossen.",
                        StringUtils.quote(txKey)
                );

        var nodeData = new LinkedHashMap<String, Object>();
        nodeData.put(DATA_KEY_RECIPIENT_EMAIL, context.getThisTask().getRuntimeData().get(DATA_KEY_RECIPIENT_EMAIL));
        nodeData.put(DATA_KEY_PAYMENT_TRANSACTION, tx.getKey());
        nodeData.put(DATA_KEY_PAYMENT_PAYLOAD, context.getThisTask().getRuntimeData().get(DATA_KEY_PAYMENT_PAYLOAD));
        nodeData.put(DATA_KEY_PAYMENT_DETAILS, tx.getPaymentInformation());

        return ProcessNodeExecutionResultTaskCompleted
                .of(PORT_NAME)
                .setNodeData(nodeData)
                .setProcessData(context.getThisTask().getProcessData());
    }

    @Nonnull
    @Override
    public GroupLayoutElement getCustomerTaskView(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        return createPaymentView(context);
    }

    @Nonnull
    private GroupLayoutElement createPaymentView(@Nonnull ProcessNodeExecutionContextUICustomer context) throws ResponseException {
        var paymentTransactionKey = context
                .getThisTask()
                .getRuntimeData()
                .get(DATA_KEY_PAYMENT_TRANSACTION);

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
                .get(DATA_KEY_PAYMENT_PAYLOAD);
        var paymentPayload = ObjectMapperFactory
                .getInstance()
                .convertValue(paymentPayloadRawData, PaymentPayload.class);

        var paymentProvider = paymentProviderRepository
                .findById(transaction.get().getPaymentProviderKey())
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Der Zahlungsanbieter mit dem Schlüssel %s konnte nicht gefunden werden".formatted(
                                StringUtils.quote(transaction.get().getPaymentProviderKey().toString())
                        )
                ));

        try {
            return new PaymentGroupPreset(paymentProvider, paymentPayload, transaction.get());
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
    private PaymentConfigElementValue requirePaymentConfig(@Nonnull PaymentRequestActionNodeConfig config) throws ProcessNodeExecutionExceptionInvalidConfiguration {
        if (config.payment == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Konfiguration der Zahlungsanforderung enthält keine Zahlungskonfiguration."
            );
        }
        if (config.payment.paymentProviderKey() == null) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Die Zahlungskonfiguration enthält keinen Zahlungsanbieter."
            );
        }
        return config.payment;
    }

    @Nonnull
    private String resolveRecipientEmail(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
                                         @Nonnull PaymentRequestActionNodeConfig config) throws ProcessNodeExecutionException {
        if (StringUtils.isNullOrEmpty(config.recipientEmailProcessDataKey)) {
            throw new ProcessNodeExecutionExceptionInvalidConfiguration(
                    "Der Prozessdaten-Schlüssel für die Empfänger-E-Mail-Adresse wurde nicht konfiguriert."
            );
        }

        var rawEmail = ProcessDataValueUtils.resolveProcessDataValue(
                context.getCurrentProcessExecutionData(),
                config.recipientEmailProcessDataKey
        );
        var email = StringUtils.toNullableTrimmedString(rawEmail);
        if (email == null) {
            throw new ProcessNodeExecutionExceptionMissingValue(
                    "Die Empfänger-E-Mail-Adresse im Prozessdaten-Schlüssel %s fehlt.",
                    StringUtils.quote(config.recipientEmailProcessDataKey)
            );
        }

        try {
            var addresses = InternetAddress.parse(email, true);
            if (addresses.length != 1) {
                throw new AddressException("Expected exactly one email address");
            }
            addresses[0].validate();
            return addresses[0].getAddress();
        } catch (AddressException e) {
            throw new ProcessNodeExecutionExceptionInvalidDataType(
                    e,
                    "Der Wert im Prozessdaten-Schlüssel %s ist keine gültige E-Mail-Adresse.",
                    StringUtils.quote(config.recipientEmailProcessDataKey)
            );
        }
    }

    private void sendPaymentRequestMail(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context,
                                        @Nonnull String recipientEmail,
                                        @Nonnull String paymentProviderName,
                                        @Nonnull PaymentPayload paymentPayload,
                                        @Nonnull String customerTaskUrl) throws ProcessNodeExecutionException {
        var processId = context.getThisProcessInstance().getProcessId();
        var process = retrieveProcess(processId);

        var department = departmentService
                .retrieve(process.getDepartmentId())
                .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                        "Die Organisationseinheit mit der ID %d konnte nicht gefunden werden.",
                        process.getDepartmentId()
                ));

        ThemeEntity theme = departmentService.getDepartmentTheme(department);

        var mailData = new HashMap<String, Object>();
        mailData.put("process", process);
        mailData.put("processInstance", context.getThisProcessInstance());
        mailData.put("processInstanceTask", context.getThisTask());
        mailData.put("taskName", context.getThisNode().resolveName(this));
        mailData.put("paymentProviderName", paymentProviderName);
        mailData.put("paymentTotalLabel", NumberUtils.formatGermanNumber(paymentPayload.getTotal(), 2));
        mailData.put("paymentPath", customerTaskUrl);

        var subject = "[Gover] " +
                (context.getThisProcessInstance().getCreatedForTestClaimId() != null ? "[Test] " : "") +
                "Zahlung erforderlich";

        try {
            mailService.sendMail(
                    theme,
                    recipientEmail,
                    Optional.empty(),
                    Optional.empty(),
                    subject,
                    MailTemplate.ProcessPaymentRequested,
                    mailData,
                    Optional.empty()
            );
        } catch (MessagingException | MailException | IOException | ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Beim Versenden der Zahlungsanforderung ist ein Fehler aufgetreten: %s",
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private ProcessEntity retrieveProcess(@Nonnull Integer processId) throws ProcessNodeExecutionException {
        try {
            return processService
                    .retrieve(processId)
                    .orElseThrow(() -> new ProcessNodeExecutionExceptionInvalidConfiguration(
                            "Der Prozess mit der ID %d konnte nicht gefunden werden.",
                            processId
                    ));
        } catch (ResponseException e) {
            throw new ProcessNodeExecutionExceptionUnknown(
                    e,
                    "Der Prozess mit der ID %d konnte nicht geladen werden: %s",
                    processId,
                    e.getMessage()
            );
        }
    }

    @Nonnull
    private String createCustomerTaskUrl(@Nonnull ProcessNodeExecutionInitContext<PaymentRequestActionNodeConfig> context) {
        return goverConfig.createUrl(
                "/process/",
                context.getThisProcessInstance().getAccessKey(),
                "tasks",
                context.getThisTask().getAccessKey()
        );
    }

    @Nonnull
    @Override
    public Class<PaymentRequestActionNodeConfig> getNodeConfigurationClass() {
        return PaymentRequestActionNodeConfig.class;
    }

    @LayoutElementPOJOBinding(id = NODE_KEY, type = ElementType.ConfigLayout)
    public static class PaymentRequestActionNodeConfig {
        public static final String RECIPIENT_EMAIL_PROCESS_DATA_KEY_FIELD_ID = "recipientEmailProcessDataKey";
        public static final String PAYMENT_FIELD_ID = "payment";

        @InputElementPOJOBinding(id = RECIPIENT_EMAIL_PROCESS_DATA_KEY_FIELD_ID, type = ElementType.ProcessDataKeyInput, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Prozessdaten-Schlüssel der Empfänger-E-Mail-Adresse"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Der Wert aus diesem Prozessdaten-Schlüssel wird als Empfänger:in für die Zahlungsanforderung verwendet."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public String recipientEmailProcessDataKey;

        @InputElementPOJOBinding(id = PAYMENT_FIELD_ID, type = ElementType.PaymentConfig, properties = {
                @ElementPOJOBindingProperty(key = "label", strValue = "Zahlung"),
                @ElementPOJOBindingProperty(key = "hint", strValue = "Definieren Sie die Zahlungspositionen und den Zahlungsanbieter."),
                @ElementPOJOBindingProperty(key = "required", boolValue = true)
        })
        public PaymentConfigElementValue payment;
    }
}
