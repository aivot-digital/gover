package de.aivot.prosuna.backend.storage.services;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.QueryTimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class StorageSyncWorkerTest {
    private final StorageSyncService service = mock(StorageSyncService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final StorageSyncWorker worker = new StorageSyncWorker(service, rabbit);

    @Test
    void listenerRunsTheRequestedSynchronization() {
        worker.listen(7);

        verify(service).syncRequestedStorageProvider(7);
    }

    @Test
    void failedSynchronizationIsNotHandedBackToTheBroker() {
        doThrow(new QueryTimeoutException("database busy")).when(service).syncRequestedStorageProvider(7);

        assertDoesNotThrow(() -> worker.listen(7));
    }

    @Test
    void unavailableBrokerDoesNotFailTheStoredRequest() {
        doThrow(new AmqpConnectException(new IllegalStateException("broker unavailable"))).when(rabbit)
                .convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, 7);

        assertDoesNotThrow(() -> worker.publish(7));
    }
}
