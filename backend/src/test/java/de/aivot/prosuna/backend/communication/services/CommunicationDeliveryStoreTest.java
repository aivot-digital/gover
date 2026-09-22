package de.aivot.prosuna.backend.communication.services;

import de.aivot.prosuna.backend.communication.entities.*;
import de.aivot.prosuna.backend.communication.exceptions.CommunicationAlreadyPendingException;
import de.aivot.prosuna.backend.communication.models.CommunicationDeliveryStatus;
import de.aivot.prosuna.backend.communication.repositories.CommunicationDeliveryRepository;
import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.process.entities.*;
import de.aivot.prosuna.backend.process.enums.*;
import de.aivot.prosuna.backend.process.repositories.*;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommunicationDeliveryStoreTest {
    private final CommunicationDeliveryRepository deliveries = mock(CommunicationDeliveryRepository.class);
    private final ProcessInstanceTaskRepository tasks = mock(ProcessInstanceTaskRepository.class);
    private final ProcessInstanceRepository instances = mock(ProcessInstanceRepository.class);
    private final CommunicationDeliveryStore store = new CommunicationDeliveryStore(deliveries, tasks, instances);

    @Test
    void recordsAttemptAndWaitStateBeforeSendingAndRejectsDuplicate() {
        var provider = new CommunicationProviderEntity();
        provider.setId(1);
        provider.setConfiguration(new AuthoredElementValues());
        provider.setCommunicationProviderDefinitionKey("zbp");
        provider.setCommunicationProviderDefinitionVersion(1);
        var task = new ProcessInstanceTaskEntity().setId(2L).setProcessInstanceId(3L).setStatus(ProcessTaskStatus.Running);
        when(tasks.findByIdForUpdate(2L)).thenReturn(Optional.of(task));
        when(deliveries.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var delivery = store.begin(provider, 2L, Map.of("version", 1));
        assertEquals(ProcessTaskStatus.AwaitingCommunication, task.getStatus());
        assertEquals(CommunicationDeliveryStatus.Sending, delivery.getStatus());
        assertNotSame(provider.getConfiguration(), delivery.getConfiguration());
        assertEquals(Map.of("version", 1), delivery.getContinuation());
        assertThrows(CommunicationAlreadyPendingException.class, () -> store.begin(provider, 2L, Map.of()));
        verify(deliveries, times(1)).saveAndFlush(any());
    }

    @Test
    void receiptSurvivesIndependentStatusQueries() {
        var id = UUID.randomUUID();
        var delivery = new CommunicationDeliveryEntity().setId(id).setStatus(CommunicationDeliveryStatus.Sending);
        when(deliveries.findByIdForUpdate(id)).thenReturn(Optional.of(delivery));
        store.submitted(id, Map.of("submissionId", "submission", "caseId", "case"));
        assertEquals(CommunicationDeliveryStatus.Submitted, delivery.getStatus());
        assertEquals("case", delivery.getReceipt().get("caseId"));
        assertNotNull(delivery.getNextCheckAt());
    }

    @Test
    void outboxCreatesOnlyOneSuccessorForRepeatedQueueDelivery() {
        var id = UUID.randomUUID();
        var delivery = new CommunicationDeliveryEntity().setId(id).setProcessInstanceId(1L).setContinuationApplied(true);
        delivery.setNextWork(Map.of("nextNodeId", 5));
        when(deliveries.findByIdForUpdate(id)).thenReturn(Optional.of(delivery));
        when(instances.findByIdForUpdate(1L)).thenReturn(Optional.of(new ProcessInstanceEntity().setStatus(ProcessInstanceStatus.Running)));
        Supplier<ProcessInstanceTaskEntity> create = mock(Supplier.class);
        when(create.get()).thenReturn(new ProcessInstanceTaskEntity().setId(3L));
        assertNotNull(store.claimNextTask(id, create));
        assertNull(store.claimNextTask(id, create));
        assertEquals(3L, delivery.getNextTaskId());
        assertNull(delivery.getNextWork());
        verify(create, times(1)).get();
    }
}
