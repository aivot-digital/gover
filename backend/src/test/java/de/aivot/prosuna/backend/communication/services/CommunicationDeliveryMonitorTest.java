package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.CommunicationDeliveryEntity;
import de.aivot.prosuna.backend.communication.models.*;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import de.aivot.prosuna.backend.core.jackson.JsonMapperTestUtils;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.*;
import de.aivot.prosuna.backend.process.models.*;
import de.aivot.prosuna.backend.process.models.executionResult.*;
import de.aivot.prosuna.backend.process.repositories.*;
import de.aivot.prosuna.backend.process.services.*;
import de.aivot.prosuna.backend.process.workers.ProcessNodeExecutionResultHandler;
import de.aivot.prosuna.backend.user.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommunicationDeliveryMonitorTest {
    private final CommunicationDeliveryRepository deliveries = mock(CommunicationDeliveryRepository.class);
    private final CommunicationProviderDefinitionService definitions = mock(CommunicationProviderDefinitionService.class);
    private final CommunicationProviderConfigurationService configurations = mock(CommunicationProviderConfigurationService.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final ProcessNodeRepository nodes = mock(ProcessNodeRepository.class);
    private final ProcessNodeDefinitionService nodeDefinitions = mock(ProcessNodeDefinitionService.class);
    private final ProcessNodeExecutionResultHandler handler = mock(ProcessNodeExecutionResultHandler.class);
    private final ProcessNodeExecutionLoggerFactory loggers = mock(ProcessNodeExecutionLoggerFactory.class);
    private final DeliveryTrackingCommunicationProvider<Object, Object> tracker = mock(DeliveryTrackingCommunicationProvider.class);
    private final de.aivot.prosuna.backend.process.workers.ProcessWorker worker = mock(de.aivot.prosuna.backend.process.workers.ProcessWorker.class);
    private final CommunicationDeliveryMonitor monitor = new CommunicationDeliveryMonitor(deliveries, definitions, configurations,
            tasks, instances, nodes, nodeDefinitions, handler, loggers, mock(UserService.class), JsonMapperTestUtils.createMapper(), worker);
    private CommunicationDeliveryEntity delivery;
    private ProcessInstanceTaskEntity task;
    private ProcessInstanceEntity instance;

    @BeforeEach
    void setup() throws Exception {
        delivery = new CommunicationDeliveryEntity().setId(UUID.randomUUID()).setTaskId(2L).setProcessInstanceId(1L)
                .setStatus(CommunicationDeliveryStatus.Submitted).setCreated(Instant.now()).setNextCheckAt(Instant.EPOCH)
                .setCheckFailures(0).setDefinitionKey("zbp").setDefinitionVersion(1).setReceipt(Map.of("submissionId", "submission"));
        task = new ProcessInstanceTaskEntity().setId(2L).setProcessInstanceId(1L).setProcessNodeId(3)
                .setStatus(ProcessTaskStatus.AwaitingCommunication);
        instance = new ProcessInstanceEntity().setId(1L).setStatus(ProcessInstanceStatus.Running);
        when(deliveries.findByIdForUpdate(delivery.getId())).thenReturn(Optional.of(delivery));
        when(tasks.findByIdForUpdate(2L)).thenReturn(Optional.of(task));
        when(instances.findByIdForUpdate(1L)).thenReturn(Optional.of(instance));
        doReturn(Optional.of(tracker)).when(definitions).retrieveProviderDefinition("zbp", 1);
        when(configurations.mapProviderConfiguration(any(), any())).thenReturn(new Object());
        when(loggers.create(anyLong(), any(), any(), any())).thenReturn(mock(ProcessNodeExecutionLogger.class));
    }

    @Test
    void submittedDoesNotAdvanceOrResend() throws Exception {
        when(tracker.checkDelivery(any(), any())).thenReturn(new CommunicationSendResult(CommunicationDeliveryStatus.Submitted, delivery.getReceipt()));
        monitor.check(delivery.getId());
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        assertFalse(delivery.getContinuationApplied());
        assertNotNull(delivery.getNextCheckAt());
        verifyNoInteractions(handler);
    }

    @Test
    void rejectionPreservesProblemAndFailsTaskAndInstance() throws Exception {
        var receipt = Map.<String, Object>of("submissionId", "submission", "problems", java.util.List.of(Map.of("detail", "Schema violation", "instance", "metadata")));
        when(tracker.checkDelivery(any(), any())).thenReturn(new CommunicationSendResult(CommunicationDeliveryStatus.Rejected, receipt));
        monitor.check(delivery.getId());
        assertEquals(ProcessTaskStatus.Failed, task.getStatus());
        assertEquals(ProcessInstanceStatus.Failed, instance.getStatus());
        assertEquals(receipt, delivery.getReceipt());
        assertNull(delivery.getNextCheckAt());
        verifyNoInteractions(handler);
    }

    @Test
    void pollingFailureKeepsTaskOpenAndRetries() throws Exception {
        when(tracker.checkDelivery(any(), any())).thenThrow(new RuntimeException("network"));
        monitor.check(delivery.getId());
        assertEquals(CommunicationDeliveryStatus.Submitted, delivery.getStatus());
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        assertEquals(1, delivery.getCheckFailures());
        assertNotNull(delivery.getNextCheckAt());
        verifyNoInteractions(handler);
    }

    @Test
    void interruptedSendWithoutReceiptIsUnknownAndNeverResent() throws Exception {
        delivery.setStatus(CommunicationDeliveryStatus.Sending).setReceipt(Map.of());
        monitor.check(delivery.getId());
        assertEquals(CommunicationDeliveryStatus.Unknown, delivery.getStatus());
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        assertNull(delivery.getNextCheckAt());
        verifyNoInteractions(tracker, handler);
    }

    @Test
    void abortedInstanceDoesNotResumeAfterDelayedAcceptance() throws Exception {
        instance.setStatus(ProcessInstanceStatus.Aborted);
        monitor.check(delivery.getId());
        assertEquals(CommunicationDeliveryStatus.Cancelled, delivery.getStatus());
        verifyNoInteractions(tracker, handler);
    }

    @Test
    void acceptedSnapshotIsAppliedOnceAfterRestart() throws Exception {
        var result = new ProcessNodeExecutionResultPaymentRequested("existing-transaction", "Payment")
                .setRuntimeData(Map.of("transaction", "existing-transaction"))
                .setCommunicationRequest(new ProcessNodeExecutionResultCommunicationRequest("citizen", CommunicationMessage.of("Subject", "Body", "Body")));
        var snapshot = CommunicationContinuation.from(result, null, null, Map.of(), Map.of());
        delivery.setContinuation(JsonMapperTestUtils.createMapper().convertValue(snapshot, Map.class));
        var node = new ProcessNodeEntity();
        when(nodes.findById(3)).thenReturn(Optional.of(node));
        doReturn(Optional.of(mock(ProcessNodeDefinition.class))).when(nodeDefinitions).getProcessNodeDefinition(node);
        when(tracker.checkDelivery(any(), any())).thenReturn(new CommunicationSendResult(CommunicationDeliveryStatus.Accepted, delivery.getReceipt()));
        monitor.check(delivery.getId());
        monitor.check(delivery.getId());
        assertTrue(delivery.getContinuationApplied());
        verify(worker, times(1)).resumeAfterConfirmedCommunication(any(), eq(instance), eq(task), eq(node), any(), any());
        verify(handler, times(1)).handleConfirmedCommunication(any(), isNull(), any(), eq(node), eq(instance), eq(task), isNull(),
                argThat(r -> r instanceof ProcessNodeExecutionResultPaymentRequested p && p.getTransactionKey().equals("existing-transaction")
                        && r.getCommunicationRequest() == null), eq(Map.of()), any());
        assertNull(delivery.getNextCheckAt());
    }

    @Test
    void pausedProcessRetainsAcceptanceUntilResumed() throws Exception {
        instance.setStatus(ProcessInstanceStatus.Paused);
        when(tracker.checkDelivery(any(), any())).thenReturn(new CommunicationSendResult(CommunicationDeliveryStatus.Accepted, delivery.getReceipt()));
        monitor.check(delivery.getId());
        assertEquals(CommunicationDeliveryStatus.Accepted, delivery.getStatus());
        assertNotNull(delivery.getNextCheckAt());
        verifyNoInteractions(handler);
    }
}
