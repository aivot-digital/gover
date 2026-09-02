package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.mail.enums.MailTemplate;
import de.aivot.prosuna.backend.mail.services.MailService;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentPayload;
import de.aivot.prosuna.backend.payment.models.PaymentProviderDefinition;
import de.aivot.prosuna.backend.payment.models.PaymentTaskRuntimeDataKeys;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.repositories.PaymentProviderRepository;
import de.aivot.prosuna.backend.payment.services.PaymentPayloadCreationService;
import de.aivot.prosuna.backend.payment.services.PaymentProviderDefinitionsService;
import de.aivot.prosuna.backend.payment.services.PaymentTransactionService;
import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionIO;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultNoop;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultPaymentRequested;
import de.aivot.prosuna.backend.process.models.executionResult.ProcessNodeExecutionResultTaskCompleted;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeDefinitionConfigurationLayoutContext;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionContextUICustomer;
import de.aivot.prosuna.backend.process.models.processContext.ProcessNodeExecutionInitContext;
import de.aivot.prosuna.backend.process.services.ProcessService;
import de.aivot.prosuna.backend.process.services.TemplateRenderService;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRequestActionNodeV1Test {
    private static final Integer PROCESS_ID = 42;
    private static final Integer PROCESS_VERSION = 3;
    private static final Integer NODE_ID = 123;
    private static final Long PROCESS_INSTANCE_ID = 99L;
    private static final Long TASK_ID = 456L;

    private PaymentPayloadCreationService paymentPayloadCreationService;
    private PaymentTransactionService paymentTransactionService;
    private PaymentProviderRepository paymentProviderRepository;
    private PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private MailService mailService;
    private ProcessService processService;
    private DepartmentService departmentService;
    private PaymentRequestActionNodeV1 node;

    @BeforeEach
    void setUp() {
        paymentPayloadCreationService = mock(PaymentPayloadCreationService.class);
        paymentTransactionService = mock(PaymentTransactionService.class);
        paymentProviderRepository = mock(PaymentProviderRepository.class);
        paymentProviderDefinitionsService = mock(PaymentProviderDefinitionsService.class);
        mailService = mock(MailService.class);
        processService = mock(ProcessService.class);
        departmentService = mock(DepartmentService.class);
        node = new PaymentRequestActionNodeV1(
                paymentPayloadCreationService,
                paymentTransactionService,
                paymentProviderRepository,
                paymentProviderDefinitionsService,
                new TemplateRenderService(new JavascriptEngineFactoryService(List.of())),
                prosunaConfig(),
                mailService,
                processService,
                departmentService,
                JsonMapperTestUtils.createMapper()
        );
    }

    @Test
    void getConfigurationLayout_ShouldExposePaymentAndRecipientEmail() throws Exception {
        var layout = node.getConfigurationLayout(configurationLayoutContext());

        assertTrue(layout.findChild(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID, PaymentConfigElement.class).isPresent());
        assertNotNull(layout.findChild(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_EMAIL_FIELD_ID).orElseThrow());
    }

    @Test
    void metadata_ShouldDescribeSemiAutomaticPaymentRequest() {
        assertArrayEquals(
                new ProcessNodeExecutionType[]{ProcessNodeExecutionType.SemiAutomatic},
                node.getExecutionTypes()
        );
        assertEquals(
                "Fordert eine Online-Zahlung an und wartet auf deren erfolgreichen Abschluss.",
                node.getAbstract()
        );
    }

    @Test
    void getOutputs_ShouldMatchNodeDataKeys() {
        var outputs = node.getOutputs();
        var outputKeys = outputs
                .stream()
                .map(output -> output.key())
                .toList();

        assertEquals(List.of(
                "recipientEmail",
                "paymentUrl",
                "paymentProviderName",
                "paymentTransactionKey",
                "paymentPurpose",
                "paymentDescription",
                "paymentTotal",
                "paymentStatus",
                "paymentDetails"
        ), outputKeys);

        var outputTypes = outputs
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        output -> output.key(),
                        output -> output.typeDefinition()
                ));

        assertEquals(Map.ofEntries(
                Map.entry("recipientEmail", "string"),
                Map.entry("paymentUrl", "string"),
                Map.entry("paymentProviderName", "string"),
                Map.entry("paymentTransactionKey", "string"),
                Map.entry("paymentPurpose", "string"),
                Map.entry("paymentDescription", "string"),
                Map.entry("paymentTotal", "number"),
                Map.entry("paymentStatus", "\"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\""),
                Map.entry(
                        "paymentDetails",
                        "{ transactionUrl: string | null; transactionRedirectUrl: string | null; " +
                                "transactionId: string | null; transactionReference: string | null; " +
                                "transactionTimestamp: string | null; " +
                                "paymentMethod: \"GIROPAY\" | \"PAYDIRECT\" | \"CREDITCARD\" | \"PAYPAL\" | \"OTHER\" | null; " +
                                "paymentMethodDetail: string | null; " +
                                "status: \"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\" | null; " +
                                "statusDetail: string | null; } | null"
                )
        ), outputTypes);
    }

    @Test
    void init_CreatesTransactionSendsMailAndRequestsPayment() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = paymentConfig(paymentProviderKey);
        var nodeConfig = nodeConfiguration(paymentConfig, "{{ $.email }}");
        var paymentProvider = paymentProvider(paymentProviderKey);
        var paymentPayload = paymentPayload();
        var transaction = paymentTransaction(paymentProviderKey, XBezahldienstStatus.INITIAL)
                .setRedirectUrl("https://example.test/process/instance-access/tasks/task-access");
        var processData = new ProcessExecutionData()
                .addProcessData(Map.of("email", "ada@example.test"));
        var process = process();
        var department = new DepartmentEntity().setId(process.getDepartmentId()).setName("Amt");
        var theme = new ThemeEntity().setName("Theme");

        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider));
        when(paymentPayloadCreationService.createRequest(eq(paymentConfig), any(DerivedRuntimeElementData.class), same(processData)))
                .thenReturn(Optional.of(paymentPayload));
        when(paymentTransactionService.create(paymentProvider, paymentPayload, "https://example.test/process/instance-access/tasks/task-access"))
                .thenReturn(transaction);
        when(processService.retrieve(PROCESS_ID))
                .thenReturn(Optional.of(process));
        when(departmentService.retrieve(process.getDepartmentId()))
                .thenReturn(Optional.of(department));
        when(departmentService.getDepartmentTheme(department))
                .thenReturn(theme);

        var result = assertInstanceOf(
                ProcessNodeExecutionResultPaymentRequested.class,
                node.init(context(nodeConfig, processData, task()))
        );

        assertEquals("tx-1", result.getTransactionKey());
        assertEquals("Stadtkasse", result.getPaymentProviderName());
        assertEquals("tx-1", result.getRuntimeData().get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY));
        assertEquals(paymentPayload, result.getRuntimeData().get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD));
        assertEquals("ada@example.test", result.getNodeData().get("recipientEmail"));
        assertEquals("https://example.test/process/instance-access/tasks/task-access", result.getNodeData().get("paymentUrl"));
        assertEquals("INITIAL", result.getNodeData().get("paymentStatus"));
        assertEquals(Map.of("email", "ada@example.test"), result.getProcessData());
        verify(mailService).sendMail(
                same(theme),
                eq("ada@example.test"),
                eq(Optional.empty()),
                eq(Optional.empty()),
                eq("[Prosuna] Zahlungsaufforderung"),
                eq(MailTemplate.ProcessPaymentRequested),
                anyMap(),
                eq(Optional.empty())
        );
    }

    @Test
    void init_FailsWhenPaymentPayloadIsEmpty() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = paymentConfig(paymentProviderKey);
        var paymentProvider = paymentProvider(paymentProviderKey);
        var processData = new ProcessExecutionData();

        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider));
        when(paymentPayloadCreationService.createRequest(eq(paymentConfig), any(DerivedRuntimeElementData.class), same(processData)))
                .thenReturn(Optional.empty());

        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(context(nodeConfiguration(paymentConfig, "ada@example.test"), processData, task()))
        );
        verify(paymentTransactionService, never()).create(any(), any(), any());
        verify(mailService, never()).sendMail(any(), any(), any(), any(), any(), any(), anyMap(), any());
    }

    @Test
    void init_FailsWhenRecipientEmailIsInvalid() {
        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(context(nodeConfiguration(paymentConfig(UUID.randomUUID()), "not-an-email"), new ProcessExecutionData(), task()))
        );
    }

    @Test
    void resume_ReturnsNoopWhilePaymentIsPending() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        when(paymentTransactionService.retrieve("tx-1"))
                .thenReturn(Optional.of(paymentTransaction(paymentProviderKey, XBezahldienstStatus.INITIAL)));

        var result = node.resume(context(
                nodeConfiguration(paymentConfig(paymentProviderKey), "ada@example.test"),
                new ProcessExecutionData(),
                task(Map.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, "tx-1"), Map.of(), Map.of())
        ));

        assertInstanceOf(ProcessNodeExecutionResultNoop.class, result);
    }

    @Test
    void resume_CompletesWhenPaymentIsPaid() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentInformation = paymentInformation(XBezahldienstStatus.PAYED);
        when(paymentTransactionService.retrieve("tx-1"))
                .thenReturn(Optional.of(paymentTransaction(paymentProviderKey, paymentInformation)));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskCompleted.class,
                node.resume(context(
                        nodeConfiguration(paymentConfig(paymentProviderKey), "ada@example.test"),
                        new ProcessExecutionData(),
                        task(
                                Map.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, "tx-1"),
                                Map.of("paymentStatus", "INITIAL", "paymentDetails", "initial-details"),
                                Map.of("existing", "value")
                        )
                ))
        );

        assertEquals("paid", result.getViaPort());
        assertEquals("PAYED", result.getNodeData().get("paymentStatus"));
        assertEquals(paymentInformation, result.getNodeData().get("paymentDetails"));
        assertEquals(Map.of("existing", "value"), result.getProcessData());
    }

    @Test
    void resume_FailsWhenPaymentFailed() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var transaction = paymentTransaction(paymentProviderKey, XBezahldienstStatus.FAILED)
                .setPaymentError("Provider rejected payment");
        when(paymentTransactionService.retrieve("tx-1"))
                .thenReturn(Optional.of(transaction));

        assertThrows(
                ProcessNodeExecutionExceptionIO.class,
                () -> node.resume(context(
                        nodeConfiguration(paymentConfig(paymentProviderKey), "ada@example.test"),
                        new ProcessExecutionData(),
                        task(Map.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, "tx-1"), Map.of(), Map.of())
                ))
        );
    }

    @Test
    void getCustomerTaskView_ShouldRenderGenericPaymentConfirmationDownloadUrl() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = new PaymentConfigElementValue(
                paymentProviderKey,
                null,
                null,
                false,
                null,
                List.of(),
                "# Danke **{{ $.name }}**.",
                null
        );
        var task = task(
                Map.of(
                        PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, "tx-1",
                        PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD, paymentPayload()
                ),
                Map.of(),
                Map.of("name", "Ada")
        );

        when(paymentTransactionService.retrieve("tx-1"))
                .thenReturn(Optional.of(paymentTransaction(paymentProviderKey, XBezahldienstStatus.PAYED)));
        var paymentProvider = paymentProvider(paymentProviderKey);
        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider));
        var paymentProviderDefinition = mock(PaymentProviderDefinition.class);
        when(paymentProviderDefinition.getProviderName()).thenReturn("Testprovider");
        when(paymentProviderDefinitionsService.getProviderDefinition(
                paymentProvider.getPaymentProviderDefinitionKey(),
                paymentProvider.getPaymentProviderDefinitionVersion()
        )).thenReturn(Optional.of(paymentProviderDefinition));

        var layout = node.getCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                logger(),
                processNode(),
                processInstance(),
                task,
                null,
                null,
                nodeConfiguration(paymentConfig, "ada@example.test"),
                null
        ));

        var downloadButton = layout.findChild("download", LinkButtonContentElement.class).orElseThrow();
        assertEquals(
                "https://example.test/api/public/processes/instance-access/tasks/task-access/payment-confirmation/",
                downloadButton.getHref()
        );
    }

    private static ProcessNodeExecutionInitContext<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> context(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig configuration,
                                                                                                                       ProcessExecutionData processData,
                                                                                                                       ProcessInstanceTaskEntity task) {
        return new ProcessNodeExecutionInitContext<>(
                logger(),
                processNode(),
                processInstance(),
                task,
                null,
                processData,
                configuration
        );
    }

    private static PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig nodeConfiguration(PaymentConfigElementValue paymentConfig,
                                                                                               String recipientEmail) {
        var configuration = new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig();
        configuration.payment = paymentConfig;
        configuration.recipientEmail = recipientEmail;
        return configuration;
    }

    private static PaymentConfigElementValue paymentConfig(UUID paymentProviderKey) {
        return new PaymentConfigElementValue(
                paymentProviderKey,
                "Zahlung",
                "Beschreibung",
                false,
                null,
                List.of(),
                null,
                null
        );
    }

    private static PaymentPayload paymentPayload() {
        return new PaymentPayload()
                .setPurpose("Zahlung")
                .setDescription("Beschreibung")
                .setTotal(BigDecimal.valueOf(12.34))
                .setPaymentItems(List.of());
    }

    private static PaymentProviderEntity paymentProvider(UUID paymentProviderKey) {
        return new PaymentProviderEntity()
                .setKey(paymentProviderKey)
                .setPaymentProviderDefinitionKey("test-provider")
                .setPaymentProviderDefinitionVersion(1)
                .setName("Stadtkasse");
    }

    private static PaymentTransactionEntity paymentTransaction(UUID paymentProviderKey, XBezahldienstStatus status) {
        return paymentTransaction(paymentProviderKey, paymentInformation(status));
    }

    private static PaymentTransactionEntity paymentTransaction(UUID paymentProviderKey,
                                                              XBezahldienstePaymentInformation paymentInformation) {
        return new PaymentTransactionEntity()
                .setKey("tx-1")
                .setPaymentProviderKey(paymentProviderKey)
                .setPaymentInformation(paymentInformation);
    }

    private static XBezahldienstePaymentInformation paymentInformation(XBezahldienstStatus status) {
        var paymentInformation = new XBezahldienstePaymentInformation();
        paymentInformation.setStatus(status);
        paymentInformation.setTransactionRedirectUrl(URI.create("https://payment.example.test/tx-1"));
        return paymentInformation;
    }

    private static ProcessInstanceEntity processInstance() {
        var now = Instant.now();

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setCaseNumber("AZ-123")
                .setAccessKey("instance-access")
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

    private static ProcessInstanceTaskEntity task() {
        return task(Map.of(), Map.of(), Map.of());
    }

    private static ProcessInstanceTaskEntity task(Map<String, Object> runtimeData,
                                                  Map<String, Object> nodeData,
                                                  Map<String, Object> processData) {
        var now = Instant.now();

        return new ProcessInstanceTaskEntity()
                .setId(TASK_ID)
                .setAccessKey("task-access")
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

    private static ProcessNodeEntity processNode() {
        return new ProcessNodeEntity()
                .setId(NODE_ID)
                .setProcessId(PROCESS_ID)
                .setProcessVersion(PROCESS_VERSION)
                .setName("Zahlung")
                .setDataKey("payment")
                .setProcessNodeDefinitionKey(PaymentRequestActionNodeV1.NODE_KEY)
                .setProcessNodeDefinitionVersion(1)
                .setConfiguration(new AuthoredElementValues())
                .setOutputMappings(Map.of());
    }

    private static ProcessEntity process() {
        return new ProcessEntity()
                .setId(PROCESS_ID)
                .setInternalTitle("Antrag")
                .setDepartmentId(1)
                .setSlug("antrag")
                .setVersionCount(PROCESS_VERSION)
                .setDraftedVersion(PROCESS_VERSION);
    }

    private static ProsunaConfig prosunaConfig() {
        var config = new ProsunaConfig();
        config.setProsunaHostname("https://example.test/");
        return config;
    }

    private static ProcessNodeDefinitionConfigurationLayoutContext configurationLayoutContext() {
        return new ProcessNodeDefinitionConfigurationLayoutContext(
                null,
                process(),
                null,
                processNode()
        );
    }

    private static ProcessNodeExecutionLogger logger() {
        return mock(ProcessNodeExecutionLogger.class);
    }
}
