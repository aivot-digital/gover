package de.aivot.gover.backend.plugins.core.v1.nodes.actions;

import de.aivot.gover.backend.department.entities.DepartmentEntity;
import de.aivot.gover.backend.department.services.DepartmentService;
import de.aivot.gover.backend.elements.models.AuthoredElementValues;
import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.elements.form.content.RichTextContentElement;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.gover.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.gover.backend.elements.models.elements.form.input.ProcessDataKeyInputElement;
import de.aivot.gover.backend.elements.uiPresets.PaymentGroupPreset;
import de.aivot.gover.backend.enums.XBezahldienstStatus;
import de.aivot.gover.backend.identity.models.IdentityDataMap;
import de.aivot.gover.backend.mail.enums.MailTemplate;
import de.aivot.gover.backend.mail.services.MailService;
import de.aivot.gover.backend.models.config.GoverConfig;
import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.gover.backend.payment.models.PaymentItem;
import de.aivot.gover.backend.payment.models.PaymentPayload;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.gover.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.gover.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.gover.backend.payment.services.PaymentTransactionService;
import de.aivot.gover.backend.process.entities.ProcessEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
import de.aivot.gover.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidDataType;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.gover.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.gover.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.gover.backend.process.services.ProcessService;
import de.aivot.gover.backend.theme.entities.ThemeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentRequestActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Integer DEPARTMENT_ID = 7;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;
    private static final UUID PROCESS_INSTANCE_ACCESS_KEY = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TASK_ACCESS_KEY = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PAYMENT_PROVIDER_KEY = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String CUSTOMER_TASK_URL = "https://example.test/process/%s/tasks/%s"
            .formatted(PROCESS_INSTANCE_ACCESS_KEY, TASK_ACCESS_KEY);

    private PaymentPayloadCreationService paymentPayloadCreationService;
    private PaymentTransactionService paymentTransactionService;
    private PaymentProviderRepository paymentProviderRepository;
    private MailService mailService;
    private ProcessService processService;
    private DepartmentService departmentService;
    private ThemeEntity theme;
    private PaymentRequestActionNodeV1 node;

    @BeforeEach
    void setUp() {
        paymentPayloadCreationService = mock(PaymentPayloadCreationService.class);
        paymentTransactionService = mock(PaymentTransactionService.class);
        paymentProviderRepository = mock(PaymentProviderRepository.class);
        mailService = mock(MailService.class);
        processService = mock(ProcessService.class);
        departmentService = mock(DepartmentService.class);
        theme = theme();

        node = new PaymentRequestActionNodeV1(
                goverConfig(),
                paymentPayloadCreationService,
                paymentTransactionService,
                paymentProviderRepository,
                mailService,
                processService,
                departmentService
        );
    }

    @Test
    void getConfigurationLayout_ShouldExposeRecipientEmailProcessDataKeyAndPaymentConfig() throws Exception {
        var layout = node.getConfigurationLayout(configurationLayoutContext());

        var emailField = layout
                .findChild(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_EMAIL_PROCESS_DATA_KEY_FIELD_ID, ProcessDataKeyInputElement.class)
                .orElseThrow();

        assertEquals(true, emailField.getRequired());
        assertEquals(true, emailField.getDisableWildCards());
        assertTrue(layout
                .findChild(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID, PaymentConfigElement.class)
                .isPresent());
    }

    @Test
    void init_ShouldCreatePaymentTransactionSendMailAndRequestPayment() throws Exception {
        var config = configuration("contact.email");
        var processExecutionData = new ProcessExecutionData().addProcessData(Map.of(
                "contact", Map.of("email", "  buyer@example.test  "),
                "caseNumber", "AZ-1"
        ));
        var payload = paymentPayload();
        var provider = paymentProvider();
        var transaction = transaction(XBezahldienstStatus.INITIAL);
        var process = process();
        var department = department();

        when(paymentProviderRepository.findById(PAYMENT_PROVIDER_KEY)).thenReturn(Optional.of(provider));
        when(paymentPayloadCreationService.createRequest(same(config.payment), any(DerivedRuntimeElementData.class), same(processExecutionData)))
                .thenReturn(Optional.of(payload));
        when(paymentTransactionService.create(same(provider), same(payload), eq(CUSTOMER_TASK_URL)))
                .thenReturn(transaction);
        when(processService.retrieve(PROCESS_ID)).thenReturn(Optional.of(process));
        when(departmentService.retrieve(DEPARTMENT_ID)).thenReturn(Optional.of(department));
        when(departmentService.getDepartmentTheme(department)).thenReturn(theme);

        var result = assertInstanceOf(
                ProcessNodeExecutionResultPaymentRequested.class,
                node.init(context(config, processExecutionData, task(Map.of(), Map.of(), Map.of())))
        );

        assertEquals("tx-1", result.getTransactionKey());
        assertEquals("Testkasse", result.getPaymentProviderName());
        assertEquals(payload, result.getRuntimeData().get(PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_PAYLOAD));
        assertEquals("tx-1", result.getRuntimeData().get(PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_TRANSACTION));
        assertEquals("buyer@example.test", result.getRuntimeData().get(PaymentRequestActionNodeV1.DATA_KEY_RECIPIENT_EMAIL));

        @SuppressWarnings("unchecked")
        var mailDataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMail(
                same(theme),
                eq("buyer@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("[Gover] Zahlung erforderlich"),
                eq(MailTemplate.ProcessPaymentRequested),
                mailDataCaptor.capture(),
                eq(Optional.empty())
        );
        var mailData = mailDataCaptor.getValue();
        assertSame(process, mailData.get("process"));
        assertEquals("Zahlung anfordern", mailData.get("taskName"));
        assertEquals("Testkasse", mailData.get("paymentProviderName"));
        assertEquals("10,00", mailData.get("paymentTotalLabel"));
        assertEquals(CUSTOMER_TASK_URL, mailData.get("paymentPath"));
    }

    @Test
    void init_ShouldRejectInvalidRecipientEmailBeforeCreatingPayment() {
        var config = configuration("contact.email");
        var processExecutionData = new ProcessExecutionData().addProcessData(Map.of(
                "contact", Map.of("email", "not-an-email")
        ));

        assertThrows(
                ProcessNodeExecutionExceptionInvalidDataType.class,
                () -> node.init(context(config, processExecutionData, task(Map.of(), Map.of(), Map.of())))
        );

        verifyNoInteractions(paymentProviderRepository, paymentPayloadCreationService, paymentTransactionService, mailService);
    }

    @Test
    void init_ShouldCompleteWithoutTransactionOrMailWhenPaymentPayloadIsEmpty() throws Exception {
        var config = configuration("contact.email");
        var processData = Map.<String, Object>of("contact", Map.of("email", "buyer@example.test"));
        var processExecutionData = new ProcessExecutionData().addProcessData(processData);
        var provider = paymentProvider();

        when(paymentProviderRepository.findById(PAYMENT_PROVIDER_KEY)).thenReturn(Optional.of(provider));
        when(paymentPayloadCreationService.createRequest(same(config.payment), any(DerivedRuntimeElementData.class), same(processExecutionData)))
                .thenReturn(Optional.empty());

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.init(context(config, processExecutionData, task(Map.of(), Map.of(), Map.of())))
        );

        assertEquals("output", result.getViaPort());
        assertEquals(processData, result.getProcessData());
        verify(paymentTransactionService, never()).create(any(), any(), any());
        verifyNoInteractions(mailService);
    }

    @Test
    void resume_ShouldCompletePaidTransactionWithPaymentDetails() throws Exception {
        var payload = paymentPayload();
        var paymentInformation = paymentInformation(XBezahldienstStatus.PAYED);
        var transaction = transaction(XBezahldienstStatus.PAYED).setPaymentInformation(paymentInformation);
        var processData = Map.<String, Object>of("caseNumber", "AZ-1");
        var task = task(
                Map.of(
                        PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_TRANSACTION, "tx-1",
                        PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_PAYLOAD, payload,
                        PaymentRequestActionNodeV1.DATA_KEY_RECIPIENT_EMAIL, "buyer@example.test"
                ),
                Map.of(),
                processData
        );

        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.of(transaction));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.resume(context(configuration("contact.email"), new ProcessExecutionData(), task))
        );

        assertEquals("output", result.getViaPort());
        assertEquals(processData, result.getProcessData());
        assertEquals("buyer@example.test", result.getNodeData().get(PaymentRequestActionNodeV1.DATA_KEY_RECIPIENT_EMAIL));
        assertEquals("tx-1", result.getNodeData().get(PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_TRANSACTION));
        assertSame(payload, result.getNodeData().get(PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_PAYLOAD));
        assertSame(paymentInformation, result.getNodeData().get(PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_DETAILS));
    }

    @Test
    void getCustomerTaskView_ShouldReturnPaymentPresetWhenRuntimeDataContainsPayment() throws Exception {
        var payload = paymentPayload();
        var transaction = transaction(XBezahldienstStatus.INITIAL);
        var provider = paymentProvider();
        var task = task(
                Map.of(
                        PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_TRANSACTION, "tx-1",
                        PaymentRequestActionNodeV1.DATA_KEY_PAYMENT_PAYLOAD, payload
                ),
                Map.of(),
                Map.of()
        );

        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.of(transaction));
        when(paymentProviderRepository.findById(PAYMENT_PROVIDER_KEY)).thenReturn(Optional.of(provider));

        var view = node.getCustomerTaskView(new ProcessNodeExecutionContextUICustomer(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                processInstance(),
                task,
                null,
                null,
                null
        ));

        assertInstanceOf(PaymentGroupPreset.class, view);
        assertEquals("payment-group", view.getId());
        assertFalse(view.getChildren().isEmpty());
        var content = assertInstanceOf(RichTextContentElement.class, view.getChildren().getFirst());
        assertNotNull(content.getContent());
        assertTrue(content.getContent().contains("Zahlung ausstehend"));
    }

    @Test
    void cleanConfigurationForExport_ShouldRemovePaymentConfiguration() {
        var configuration = new AuthoredElementValues();
        configuration.put(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_EMAIL_PROCESS_DATA_KEY_FIELD_ID, "contact.email");
        configuration.put(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID, Map.of("paymentProviderKey", PAYMENT_PROVIDER_KEY));

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertEquals("contact.email", cleaned.get(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_EMAIL_PROCESS_DATA_KEY_FIELD_ID));
        assertFalse(cleaned.containsKey(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID));
    }

    private static PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig configuration(String recipientEmailProcessDataKey) {
        var config = new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig();
        config.recipientEmailProcessDataKey = recipientEmailProcessDataKey;
        config.payment = new PaymentConfigElementValue(
                PAYMENT_PROVIDER_KEY,
                "PAY {{ $.caseNumber }}",
                "Fee",
                false,
                null,
                List.of()
        );
        return config;
    }

    private static ProcessNodeExecutionInitContext<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> context(
            PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig configuration,
            ProcessExecutionData processExecutionData,
            ProcessInstanceTaskEntity task
    ) {
        return new ProcessNodeExecutionInitContext<>(
                mock(ProcessNodeExecutionLogger.class),
                processNode(),
                processInstance(),
                task,
                null,
                processExecutionData,
                configuration
        );
    }

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName(null)
                .setDataKey("paymentNode")
                .setProcessNodeDefinitionKey(PaymentRequestActionNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static ProcessInstanceEntity processInstance() {
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setCaseNumber("GOV-1")
                .setAccessKey(PROCESS_INSTANCE_ACCESS_KEY)
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(new IdentityDataMap())
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(NODE_ID);
    }

    private static ProcessInstanceTaskEntity task(Map<String, Object> runtimeData,
                                                  Map<String, Object> nodeData,
                                                  Map<String, Object> processData) {
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey(TASK_ACCESS_KEY)
                .setProcessInstanceId(PROCESS_INSTANCE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setProcessNodeId(NODE_ID)
                .setStatus(ProcessTaskStatus.Running)
                .setStarted(now)
                .setUpdated(now)
                .setRuntimeData(runtimeData)
                .setNodeData(nodeData)
                .setProcessData(processData);
    }

    private static ProcessNodeDefinitionConfigurationLayoutContext configurationLayoutContext() {
        return new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process(),
                processVersion(),
                processNode()
        );
    }

    private static ProcessEntity process() {
        return new ProcessEntity()
                .setId(PROCESS_ID)
                .setInternalTitle("Antrag")
                .setDepartmentId(DEPARTMENT_ID)
                .setAccessKey(UUID.randomUUID())
                .setSlug("antrag")
                .setVersionCount(PROCESS_VERSION)
                .setDraftedVersion(PROCESS_VERSION);
    }

    private static ProcessVersionEntity processVersion() {
        return new ProcessVersionEntity()
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessVersionStatus.Drafted)
                .setPublicTitle("Antrag");
    }

    private static DepartmentEntity department() {
        return new DepartmentEntity()
                .setId(DEPARTMENT_ID)
                .setName("Fachbereich")
                .setDepth(0);
    }

    private static ThemeEntity theme() {
        return new ThemeEntity()
                .setId(1)
                .setName("Default")
                .setMain("#000000")
                .setMainDark("#000000")
                .setAccent("#000000")
                .setError("#000000")
                .setWarning("#000000")
                .setInfo("#000000")
                .setSuccess("#000000");
    }

    private static PaymentProviderEntity paymentProvider() {
        return new PaymentProviderEntity()
                .setKey(PAYMENT_PROVIDER_KEY)
                .setPaymentProviderDefinitionKey("xbezahldienst")
                .setPaymentProviderDefinitionVersion(1)
                .setName("Testkasse")
                .setDescription("Test")
                .setTestProvider(true)
                .setIsEnabled(true)
                .setConfig(new AuthoredElementValues());
    }

    private static PaymentTransactionEntity transaction(XBezahldienstStatus status) {
        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentProviderKey(PAYMENT_PROVIDER_KEY)
                .setPaymentInformation(paymentInformation(status))
                .setRedirectUrl(CUSTOMER_TASK_URL)
                .setCreated(Instant.now())
                .setUpdated(Instant.now());
    }

    private static XBezahldienstePaymentInformation paymentInformation(XBezahldienstStatus status) {
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setStatus(status);
        paymentInformation.setTransactionRedirectUrl(URI.create("https://pay.example.test/tx-1"));
        return paymentInformation;
    }

    private static PaymentPayload paymentPayload() {
        var item = new PaymentItem();
        item.setId("fee");
        item.setReference("REF-1");
        item.setDescription("Gebuehr");
        item.setQuantity(1L);
        item.setTaxRate(BigDecimal.ZERO);
        item.setNetPrice(new BigDecimal("10.00"));

        return new PaymentPayload()
                .setPurpose("PAY AZ-1")
                .setDescription("Fee")
                .setTotal(new BigDecimal("10.00"))
                .setPaymentItems(List.of(item));
    }

    private static GoverConfig goverConfig() {
        var config = new GoverConfig();
        config.setGoverHostname("https://example.test/");
        return config;
    }
}
