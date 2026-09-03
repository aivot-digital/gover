package de.aivot.prosuna.backend.plugins.core.v1.nodes.actions;

import de.aivot.prosuna.backend.communication.exceptions.CommunicationException;
import de.aivot.prosuna.backend.communication.models.CommunicationMessage;
import de.aivot.prosuna.backend.communication.services.CommunicationService;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.form.content.LinkButtonContentElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssignmentContextInputElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.PaymentConfigElementValue;
import de.aivot.prosuna.backend.elements.models.elements.form.input.ProcessIdentityIdInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RadioInputElementOption;
import de.aivot.prosuna.backend.elements.models.elements.form.input.RichTextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.IdentityData;
import de.aivot.prosuna.backend.identity.models.IdentityDataMap;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngineFactoryService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
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
import de.aivot.prosuna.backend.process.entities.ProcessVersionEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.enums.ProcessNodeExecutionType;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionIO;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionInvalidConfiguration;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionMissingValue;
import de.aivot.prosuna.backend.process.exceptions.ProcessNodeExecutionExceptionUnknown;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import de.aivot.prosuna.backend.process.models.ProcessNodeDefinition;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.prosuna.backend.process.models.TaskViewEvent;
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
import de.aivot.prosuna.backend.user.entities.UserEntity;
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private static final String RECIPIENT_IDENTITY_ID = "applicant";

    private PaymentPayloadCreationService paymentPayloadCreationService;
    private PaymentTransactionService paymentTransactionService;
    private PaymentProviderRepository paymentProviderRepository;
    private PaymentProviderDefinitionsService paymentProviderDefinitionsService;
    private CommunicationService communicationService;
    private AssignmentContextAssigneeResolverService assignmentContextAssigneeResolverService;
    private PaymentRequestActionNodeV1 node;

    @BeforeEach
    void setUp() {
        paymentPayloadCreationService = mock(PaymentPayloadCreationService.class);
        paymentTransactionService = mock(PaymentTransactionService.class);
        paymentProviderRepository = mock(PaymentProviderRepository.class);
        paymentProviderDefinitionsService = mock(PaymentProviderDefinitionsService.class);
        communicationService = mock(CommunicationService.class);
        assignmentContextAssigneeResolverService = mock(AssignmentContextAssigneeResolverService.class);
        node = new PaymentRequestActionNodeV1(
                paymentPayloadCreationService,
                paymentTransactionService,
                paymentProviderRepository,
                paymentProviderDefinitionsService,
                new TemplateRenderService(new JavascriptEngineFactoryService(List.of())),
                prosunaConfig(),
                JsonMapperTestUtils.createMapper(),
                communicationService,
                assignmentContextAssigneeResolverService
        );
    }

    @Test
    void configurationLayout_ExposesIdentityPaymentAndBothExecutionModes() throws Exception {
        var layout = node.getConfigurationLayout(configurationLayoutContext());

        assertTrue(layout.findChild(
                PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID,
                ProcessIdentityIdInputElement.class
        ).isPresent());
        assertTrue(layout.findChild(
                PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID,
                PaymentConfigElement.class
        ).isPresent());

        var executionType = layout.findChild(
                PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.EXECUTION_TYPE_FIELD_ID,
                RadioInputElement.class
        ).orElseThrow();
        assertEquals(List.of(
                RadioInputElementOption.of("automatic", "Automatisch versenden"),
                RadioInputElementOption.of("manual", "Vor dem Versand bearbeiten")
        ), executionType.getOptions());

        var automaticGroup = layout.findChild(
                PaymentRequestActionNodeV1.AutomaticContent.GROUP_ID,
                GroupLayoutElement.class
        ).orElseThrow();
        var manualGroup = layout.findChild(
                PaymentRequestActionNodeV1.ManualContent.GROUP_ID,
                GroupLayoutElement.class
        ).orElseThrow();
        assertNotNull(automaticGroup.getVisibility());
        assertNotNull(manualGroup.getVisibility());

        var assignment = layout.findChild(
                PaymentRequestActionNodeV1.ManualContent.ASSIGNMENT_FIELD_ID,
                AssignmentContextInputElement.class
        ).orElseThrow();
        assertEquals(List.of("orgUnit", "team", "user"), assignment.getAllowedTypes());
        assertEquals(PROCESS_ID, assignment.getProcessAccessConstraint().getProcessId());
        assertEquals(PROCESS_VERSION, assignment.getProcessAccessConstraint().getProcessVersion());
        assertEquals(
                List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK),
                assignment.getProcessAccessConstraint().getRequiredPermissions()
        );
    }

    @Test
    void metadata_DescribesAutomaticAndSemiAutomaticExecution() {
        assertArrayEquals(
                new ProcessNodeExecutionType[]{
                        ProcessNodeExecutionType.Automatic,
                        ProcessNodeExecutionType.SemiAutomatic
                },
                node.getExecutionTypes()
        );
        assertFalse(node.getAbstract().isBlank());
    }

    @Test
    void outputs_MatchAllProducedNodeDataKeys() {
        var outputs = node.getOutputs();

        assertEquals(List.of(
                "recipientIdentityId",
                "paymentUrl",
                "paymentProviderName",
                "paymentTransactionKey",
                "paymentPurpose",
                "paymentDescription",
                "paymentTotal",
                "paymentStatus",
                "paymentDetails"
        ), outputs.stream().map(output -> output.key()).toList());

        var outputTypes = outputs.stream().collect(java.util.stream.Collectors.toMap(
                output -> output.key(),
                output -> output.typeDefinition()
        ));
        assertEquals("string", outputTypes.get("recipientIdentityId"));
        assertEquals("number", outputTypes.get("paymentTotal"));
        assertEquals("\"INITIAL\" | \"PAYED\" | \"FAILED\" | \"CANCELED\"", outputTypes.get("paymentStatus"));
        assertTrue(outputTypes.get("paymentDetails").startsWith("{ transactionUrl:"));
    }

    @Test
    void initAutomatic_RendersTemplatesSendsToIdentityAndRequestsPayment() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = paymentConfig(paymentProviderKey);
        var configuration = nodeConfiguration(paymentConfig, "automatic");
        var paymentProvider = paymentProvider(paymentProviderKey);
        var paymentPayload = paymentPayload();
        var transaction = paymentTransaction(paymentProviderKey, XBezahldienstStatus.INITIAL)
                .setRedirectUrl("https://example.test/process/instance-access/tasks/task-access");
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var identity = recipientIdentity();

        when(paymentProviderRepository.findById(paymentProviderKey)).thenReturn(Optional.of(paymentProvider));
        when(paymentPayloadCreationService.createRequest(
                eq(paymentConfig), any(DerivedRuntimeElementData.class), same(processData)
        )).thenReturn(Optional.of(paymentPayload));
        when(paymentTransactionService.create(
                paymentProvider, paymentPayload, "https://example.test/process/instance-access/tasks/task-access"
        )).thenReturn(transaction);
        when(communicationService.sendMessage(any(), any())).thenReturn(Map.of());

        var result = assertInstanceOf(
                ProcessNodeExecutionResultPaymentRequested.class,
                node.init(context(configuration, processData, processInstance(identity), task()))
        );

        assertEquals("tx-1", result.getTransactionKey());
        assertEquals("Stadtkasse", result.getPaymentProviderName());
        assertEquals("tx-1", result.getRuntimeData().get(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY));
        assertEquals(paymentPayload, result.getRuntimeData().get(PaymentTaskRuntimeDataKeys.PAYMENT_PAYLOAD));
        assertEquals(RECIPIENT_IDENTITY_ID, result.getNodeData().get("recipientIdentityId"));
        assertEquals("https://example.test/process/instance-access/tasks/task-access", result.getNodeData().get("paymentUrl"));
        assertEquals("INITIAL", result.getNodeData().get("paymentStatus"));
        assertEquals(Map.of("name", "Ada"), result.getProcessData());

        var messageCaptor = ArgumentCaptor.forClass(CommunicationMessage.class);
        verify(communicationService).sendMessage(same(identity), messageCaptor.capture());
        assertEquals("Zahlung für Ada", messageCaptor.getValue().subject());
        assertEquals("Hallo **Ada**", messageCaptor.getValue().body());
        assertEquals("Hallo **Ada**", messageCaptor.getValue().htmlBody());
        verify(assignmentContextAssigneeResolverService, never()).resolveAssignee(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void initManual_AssignsStaffWithoutCreatingPaymentOrSendingMessage() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var configuration = nodeConfiguration(paymentConfig(paymentProviderKey), "manual");
        var identity = recipientIdentity();
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));

        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider(paymentProviderKey)));
        when(assignmentContextAssigneeResolverService.resolveAssignee(
                eq(PROCESS_ID),
                eq(PROCESS_VERSION),
                eq(PROCESS_INSTANCE_ID),
                eq(NODE_ID),
                eq(TASK_ID),
                isNull(),
                isNull(),
                same(configuration.manualContent.assignmentContext),
                eq(List.of(ProcessPermissionProvider.PROCESS_INSTANCE_EDIT_TASK))
        )).thenReturn(Optional.of("staff-1"));

        var result = assertInstanceOf(
                ProcessNodeExecutionResultTaskAssigned.class,
                node.init(context(configuration, processData, processInstance(identity), task()))
        );

        assertEquals("staff-1", result.getAssignedUserId());
        assertEquals(Map.of("name", "Ada"), result.getProcessData());
        verify(paymentPayloadCreationService, never()).createRequest(any(), any(), any());
        verify(paymentTransactionService, never()).create(any(), any(), any());
        verify(communicationService, never()).sendMessage(any(), any());
    }

    @Test
    void staffTask_DefaultDataRendersManualTemplatesOnce() throws Exception {
        var configuration = nodeConfiguration(paymentConfig(UUID.randomUUID()), "manual");
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var context = staffContext(configuration, processData, processInstance(recipientIdentity()), task());

        var layout = node.getStaffTaskView(context);
        assertTrue(Boolean.TRUE.equals(layout.findChild("subject", TextInputElement.class).orElseThrow().getRequired()));
        assertTrue(Boolean.TRUE.equals(layout.findChild("body", RichTextInputElement.class).orElseThrow().getRequired()));
        assertEquals(
                List.of(new TaskViewEvent("Zahlungsaufforderung versenden", "send")),
                node.getStaffTaskViewEvents(context)
        );

        var defaults = node.createDefaultStaffTaskViewData(context);
        assertEquals("Entwurf für Ada", defaults.get("subject"));
        assertEquals("Bitte Ada prüfen", defaults.get("body"));
        verify(paymentPayloadCreationService, never()).createRequest(any(), any(), any());
        verify(paymentTransactionService, never()).create(any(), any(), any());
    }

    @Test
    void staffTask_SendCreatesPaymentAndSendsEditedMessageWithoutRenderingAgain() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = paymentConfig(paymentProviderKey);
        var configuration = nodeConfiguration(paymentConfig, "manual");
        var paymentProvider = paymentProvider(paymentProviderKey);
        var paymentPayload = paymentPayload();
        var transaction = paymentTransaction(paymentProviderKey, XBezahldienstStatus.INITIAL);
        var processData = new ProcessExecutionData().addProcessData(Map.of("name", "Ada"));
        var identity = recipientIdentity();
        var task = task(Map.of("existing", "runtime"), Map.of(), Map.of());

        when(paymentProviderRepository.findById(paymentProviderKey)).thenReturn(Optional.of(paymentProvider));
        when(paymentPayloadCreationService.createRequest(
                eq(paymentConfig), any(DerivedRuntimeElementData.class), same(processData)
        )).thenReturn(Optional.of(paymentPayload));
        when(paymentTransactionService.create(
                paymentProvider, paymentPayload, "https://example.test/process/instance-access/tasks/task-access"
        )).thenReturn(transaction);
        when(communicationService.sendMessage(any(), any())).thenReturn(Map.of());

        var update = authored(
                "subject", "Bearbeitet {{ $.name }}",
                "body", "Manuell **{{ $.name }}**"
        );
        var result = assertInstanceOf(
                ProcessNodeExecutionResultPaymentRequested.class,
                node.onEventFromStaffTaskView(
                        staffContext(configuration, processData, processInstance(identity), task),
                        update,
                        "send"
                ).orElseThrow()
        );

        var messageCaptor = ArgumentCaptor.forClass(CommunicationMessage.class);
        verify(communicationService).sendMessage(same(identity), messageCaptor.capture());
        assertEquals("Bearbeitet {{ $.name }}", messageCaptor.getValue().subject());
        assertEquals("Manuell **{{ $.name }}**", messageCaptor.getValue().body());
        assertEquals("runtime", result.getRuntimeData().get("existing"));
        var savedStaffData = assertInstanceOf(
                AuthoredElementValues.class,
                result.getRuntimeData().get(ProcessNodeDefinition.STAFF_TASK_VIEW_DATA_RUNTIME_KEY)
        );
        assertEquals("Bearbeitet {{ $.name }}", savedStaffData.get("subject"));
        assertEquals("Manuell **{{ $.name }}**", savedStaffData.get("body"));
    }

    @Test
    void staffTask_RejectsEmptyEditedMessageBeforeCreatingPayment() throws Exception {
        var configuration = nodeConfiguration(paymentConfig(UUID.randomUUID()), "manual");

        assertThrows(
                ResponseException.class,
                () -> node.onEventFromStaffTaskView(
                        staffContext(
                                configuration,
                                new ProcessExecutionData(),
                                processInstance(recipientIdentity()),
                                task()
                        ),
                        authored("subject", " ", "body", ""),
                        "send"
                )
        );

        verify(paymentPayloadCreationService, never()).createRequest(any(), any(), any());
        verify(paymentTransactionService, never()).create(any(), any(), any());
        verify(communicationService, never()).sendMessage(any(), any());
    }

    @Test
    void staffTask_RejectsUnknownEvent() {
        var configuration = nodeConfiguration(paymentConfig(UUID.randomUUID()), "manual");

        assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.onEventFromStaffTaskView(
                        staffContext(
                                configuration,
                                new ProcessExecutionData(),
                                processInstance(recipientIdentity()),
                                task()
                        ),
                        authored("subject", "Betreff", "body", "Nachricht"),
                        "save"
                )
        );
    }

    @Test
    void initAutomatic_FailsWhenRecipientIdentityIsMissing() throws Exception {
        var configuration = nodeConfiguration(paymentConfig(UUID.randomUUID()), "automatic");

        assertThrows(
                ProcessNodeExecutionExceptionMissingValue.class,
                () -> node.init(context(
                        configuration,
                        new ProcessExecutionData(),
                        processInstance(null),
                        task()
                ))
        );
        verify(paymentPayloadCreationService, never()).createRequest(any(), any(), any());
        verify(paymentTransactionService, never()).create(any(), any(), any());
    }

    @Test
    void initAutomatic_FailsWhenPaymentPayloadIsEmpty() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = paymentConfig(paymentProviderKey);
        var configuration = nodeConfiguration(paymentConfig, "automatic");
        var processData = new ProcessExecutionData();

        when(paymentProviderRepository.findById(paymentProviderKey))
                .thenReturn(Optional.of(paymentProvider(paymentProviderKey)));
        when(paymentPayloadCreationService.createRequest(
                eq(paymentConfig), any(DerivedRuntimeElementData.class), same(processData)
        )).thenReturn(Optional.empty());

        assertThrows(
                ProcessNodeExecutionExceptionInvalidConfiguration.class,
                () -> node.init(context(configuration, processData, processInstance(recipientIdentity()), task()))
        );
        verify(paymentTransactionService, never()).create(any(), any(), any());
        verify(communicationService, never()).sendMessage(any(), any());
    }

    @Test
    void initAutomatic_MapsCommunicationFailureToExecutionException() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        var paymentConfig = paymentConfig(paymentProviderKey);
        var configuration = nodeConfiguration(paymentConfig, "automatic");
        var paymentProvider = paymentProvider(paymentProviderKey);
        var paymentPayload = paymentPayload();
        var processData = new ProcessExecutionData();

        when(paymentProviderRepository.findById(paymentProviderKey)).thenReturn(Optional.of(paymentProvider));
        when(paymentPayloadCreationService.createRequest(
                eq(paymentConfig), any(DerivedRuntimeElementData.class), same(processData)
        )).thenReturn(Optional.of(paymentPayload));
        when(paymentTransactionService.create(any(), any(), any()))
                .thenReturn(paymentTransaction(paymentProviderKey, XBezahldienstStatus.INITIAL));
        when(communicationService.sendMessage(any(), any()))
                .thenThrow(new CommunicationException("Versand fehlgeschlagen"));

        assertThrows(
                ProcessNodeExecutionExceptionUnknown.class,
                () -> node.init(context(configuration, processData, processInstance(recipientIdentity()), task()))
        );
    }

    @Test
    void resume_ReturnsNoopWhilePaymentIsPending() throws Exception {
        var paymentProviderKey = UUID.randomUUID();
        when(paymentTransactionService.retrieve("tx-1"))
                .thenReturn(Optional.of(paymentTransaction(paymentProviderKey, XBezahldienstStatus.INITIAL)));

        var result = node.resume(context(
                nodeConfiguration(paymentConfig(paymentProviderKey), "automatic"),
                new ProcessExecutionData(),
                processInstance(recipientIdentity()),
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
                        nodeConfiguration(paymentConfig(paymentProviderKey), "automatic"),
                        new ProcessExecutionData(),
                        processInstance(recipientIdentity()),
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
        when(paymentTransactionService.retrieve("tx-1")).thenReturn(Optional.of(transaction));

        assertThrows(
                ProcessNodeExecutionExceptionIO.class,
                () -> node.resume(context(
                        nodeConfiguration(paymentConfig(paymentProviderKey), "automatic"),
                        new ProcessExecutionData(),
                        processInstance(recipientIdentity()),
                        task(Map.of(PaymentTaskRuntimeDataKeys.PAYMENT_TRANSACTION_KEY, "tx-1"), Map.of(), Map.of())
                ))
        );
    }

    @Test
    void customerTaskView_RendersPaymentConfirmationDownloadUrl() throws Exception {
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
        when(paymentProviderRepository.findById(paymentProviderKey)).thenReturn(Optional.of(paymentProvider));
        var paymentProviderDefinition = mock(PaymentProviderDefinition.class);
        when(paymentProviderDefinition.getProviderName()).thenReturn("Testprovider");
        when(paymentProviderDefinitionsService.getProviderDefinition(
                paymentProvider.getPaymentProviderDefinitionKey(),
                paymentProvider.getPaymentProviderDefinitionVersion()
        )).thenReturn(Optional.of(paymentProviderDefinition));

        var layout = node.getCustomerTaskView(new ProcessNodeExecutionContextUICustomer<>(
                logger(),
                processNode(),
                processInstance(recipientIdentity()),
                task,
                null,
                null,
                nodeConfiguration(paymentConfig, "automatic"),
                null
        ));

        var downloadButton = layout.findChild("download", LinkButtonContentElement.class).orElseThrow();
        assertEquals(
                "https://example.test/api/public/processes/instance-access/tasks/task-access/payment-confirmation/",
                downloadButton.getHref()
        );
    }

    @Test
    void cleanConfigurationForExport_RemovesPaymentAndAssignment() {
        var configuration = authored(
                PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID,
                RECIPIENT_IDENTITY_ID,
                PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID,
                Map.of("provider", "secret"),
                PaymentRequestActionNodeV1.ManualContent.ASSIGNMENT_FIELD_ID,
                Map.of("user", "staff-1")
        );

        var cleaned = node.cleanConfigurationForExport(configuration);

        assertEquals(RECIPIENT_IDENTITY_ID, cleaned.get(
                PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.RECIPIENT_IDENTITY_ID_FIELD_ID
        ));
        assertFalse(cleaned.containsKey(PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig.PAYMENT_FIELD_ID));
        assertFalse(cleaned.containsKey(PaymentRequestActionNodeV1.ManualContent.ASSIGNMENT_FIELD_ID));
    }

    private static ProcessNodeExecutionInitContext<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> context(
            PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig configuration,
            ProcessExecutionData processData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task
    ) {
        return new ProcessNodeExecutionInitContext<>(
                logger(),
                processNode(),
                processInstance,
                task,
                null,
                processData,
                configuration
        );
    }

    private static ProcessNodeExecutionContextUIStaff<PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig> staffContext(
            PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig configuration,
            ProcessExecutionData processData,
            ProcessInstanceEntity processInstance,
            ProcessInstanceTaskEntity task
    ) {
        return new ProcessNodeExecutionContextUIStaff<>(
                logger(),
                processNode(),
                processInstance,
                task,
                null,
                new UserEntity().setId("staff-1"),
                configuration,
                processData
        );
    }

    private static PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig nodeConfiguration(
            PaymentConfigElementValue paymentConfig,
            String executionType
    ) {
        var configuration = new PaymentRequestActionNodeV1.PaymentRequestActionNodeConfig();
        configuration.recipientIdentityId = RECIPIENT_IDENTITY_ID;
        configuration.payment = paymentConfig;
        configuration.executionType = executionType;

        configuration.automaticContent = new PaymentRequestActionNodeV1.AutomaticContent();
        configuration.automaticContent.subject = "Zahlung für {{ $.name }}";
        configuration.automaticContent.content = "Hallo **{{ $.name }}**";

        configuration.manualContent = new PaymentRequestActionNodeV1.ManualContent();
        configuration.manualContent.subject = "Entwurf für {{ $.name }}";
        configuration.manualContent.content = "Bitte {{ $.name }} prüfen";
        configuration.manualContent.assignmentContext = new AssignmentContextInputElementValue();
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

    private static PaymentTransactionEntity paymentTransaction(
            UUID paymentProviderKey,
            XBezahldienstStatus status
    ) {
        return paymentTransaction(paymentProviderKey, paymentInformation(status));
    }

    private static PaymentTransactionEntity paymentTransaction(
            UUID paymentProviderKey,
            XBezahldienstePaymentInformation paymentInformation
    ) {
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

    private static IdentityData recipientIdentity() {
        return new IdentityData(
                "session",
                RECIPIENT_IDENTITY_ID,
                IdentityType.Email,
                null,
                null,
                "ada@example.test",
                Map.of(),
                null,
                Map.of()
        );
    }

    private static ProcessInstanceEntity processInstance(IdentityData identity) {
        var now = Instant.now();
        var identities = new IdentityDataMap();
        if (identity != null) {
            identities.put(identity.identityId(), identity);
        }

        return new ProcessInstanceEntity()
                .setId(PROCESS_INSTANCE_ID)
                .setCaseNumber("AZ-123")
                .setAccessKey("instance-access")
                .setProcessId(PROCESS_ID)
                .setInitialProcessVersion(PROCESS_VERSION)
                .setStatus(ProcessInstanceStatus.Running)
                .setAssignedFileNumbers(List.of())
                .setIdentities(identities)
                .setStarted(now)
                .setUpdated(now)
                .setInitialPayload(Map.of())
                .setInitialNodeId(NODE_ID);
    }

    private static ProcessInstanceTaskEntity task() {
        return task(Map.of(), Map.of(), Map.of());
    }

    private static ProcessInstanceTaskEntity task(
            Map<String, Object> runtimeData,
            Map<String, Object> nodeData,
            Map<String, Object> processData
    ) {
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
                new ProcessVersionEntity()
                        .setProcessId(PROCESS_ID)
                        .setProcessVersion(PROCESS_VERSION),
                processNode()
        );
    }

    private static AuthoredElementValues authored(Object... entries) {
        var values = new AuthoredElementValues();
        for (var i = 0; i < entries.length; i += 2) {
            values.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return values;
    }

    private static ProcessNodeExecutionLogger logger() {
        return mock(ProcessNodeExecutionLogger.class);
    }
}
