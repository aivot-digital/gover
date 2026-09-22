package de.aivot.prosuna.backend.process.workers;

import de.aivot.prosuna.backend.communication.entities.*;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationDispatchException;
import de.aivot.prosuna.backend.communication.models.*;
import de.aivot.prosuna.backend.communication.services.*;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.services.DepartmentService;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.identity.entities.IdentityProviderEntity;
import de.aivot.prosuna.backend.identity.enums.IdentityType;
import de.aivot.prosuna.backend.identity.models.*;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.ProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProcessCommunicationDeliveryTest {
    private final CommunicationService communication = mock(CommunicationService.class);
    private final CommunicationDeliveryStore deliveries = mock(CommunicationDeliveryStore.class);
    private final ProcessService processes = mock(ProcessService.class);
    private final DepartmentService departments = mock(DepartmentService.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final ProcessNodeExecutionResultHandler handler = new ProcessNodeExecutionResultHandler(rabbit, communication,
            mock(ProcessInstanceRepository.class), tasks, mock(ProcessEdgeRepository.class), null, null, null, null,
            processes, departments, deliveries, JsonMapperTestUtils.createMapper());
    private final ProcessNodeExecutionLogger logger = mock(ProcessNodeExecutionLogger.class);
    private final ProcessNodeDefinition<?> definition = mock(ProcessNodeDefinition.class);
    private final UUID deliveryId = UUID.randomUUID();
    private ProcessInstanceEntity instance;
    private ProcessInstanceTaskEntity task;
    private ProcessNodeExecutionResult result;

    @BeforeEach
    void setup() throws Exception {
        var identity = new IdentityData("session", "citizen", IdentityType.Email, null, null, null, "mail@example.test", Map.of(), null, Map.of());
        var identities = new IdentityDataMap();
        identities.put("citizen", identity);
        instance = new ProcessInstanceEntity().setId(1L).setProcessId(4).setCaseNumber("VG-2026-17").setIdentities(identities);
        task = new ProcessInstanceTaskEntity().setId(2L).setStatus(ProcessTaskStatus.Running);
        var provider = new CommunicationProviderEntity();
        provider.setId(7);
        provider.setConfiguration(new AuthoredElementValues());
        var prepared = new CommunicationService.PreparedTrackedSend(new CommunicationService.ResolvedCommunicationProvider(
                mock(DeliveryTrackingCommunicationProvider.class), new CommunicationProviderContext<>(provider,
                mock(IdentityProviderEntity.class), mock(CommunicationProviderBindingEntity.class), Map.of(), Map.of())));
        when(communication.prepareTrackedSend(identity)).thenReturn(Optional.of(prepared));
        when(communication.sendPreparedTracked(any(), any(), any())).thenReturn(Map.of("submissionId", "submission"));
        when(deliveries.begin(any(), eq(2L), any())).thenReturn(new CommunicationDeliveryEntity().setId(deliveryId));
        when(processes.retrieve(4)).thenReturn(Optional.of(new ProcessEntity().setId(4).setDepartmentId(7)));
        when(departments.retrieve(7)).thenReturn(Optional.of(new DepartmentEntity().setId(7).setName("Department")));
        result = new ProcessNodeExecutionResultPaymentRequested("existing-transaction", "Payment")
                .setRuntimeData(Map.of("transaction", "existing-transaction"))
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest("citizen", CommunicationMessage.of("Subject", "Body", "Body")));
    }

    @Test
    void pendingReceiptDoesNotApplyPaymentResultOrAdvanceAndCarriesCaseNumber() throws Exception {
        handler.handleResult(logger, null, definition, new ProcessNodeEntity(), instance, task, null, result);
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        verify(deliveries).submitted(deliveryId, Map.of("submissionId", "submission"));
        verify(communication).sendPreparedTracked(any(), any(), argThat(message -> "VG-2026-17".equals(message.reference())));
        verifyNoInteractions(rabbit);
        verify(tasks, never()).save(any());
        var snapshot = ArgumentCaptor.forClass(Map.class);
        verify(deliveries).begin(any(), eq(2L), snapshot.capture());
        var continuation = JsonMapperTestUtils.createMapper().convertValue(snapshot.getValue(), CommunicationContinuation.class);
        assertEquals("existing-transaction", continuation.transactionKey());
        assertNull(continuation.restore(Map.of()).getCommunicationRequest());
    }

    @Test
    void receiptPersistenceFailureKeepsTaskWaitingRatherThanEnablingRetry() throws Exception {
        doThrow(new RuntimeException("database unavailable")).when(deliveries).submitted(any(), any());
        handler.handleResult(logger, null, definition, new ProcessNodeEntity(), instance, task, null, result);
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        verifyNoInteractions(rabbit);
        verify(tasks, never()).save(any());
    }

    @Test
    void uncertainSendAndFailedErrorPersistenceStillCannotAdvanceOrResend() throws Exception {
        when(communication.sendPreparedTracked(any(), any(), any())).thenThrow(new CommunicationDispatchException(new RuntimeException("timeout")));
        doThrow(new RuntimeException("database unavailable")).when(deliveries).failed(any(), anyBoolean(), any());
        handler.handleResult(logger, null, definition, new ProcessNodeEntity(), instance, task, null, result);
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        verify(communication, times(1)).sendPreparedTracked(any(), any(), any());
        verifyNoInteractions(rabbit);
    }
}
