package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.QueryTimeoutException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StorageSyncWorkerTest {
    private final StorageSyncService service = mock(StorageSyncService.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final StorageSyncWorker worker = new StorageSyncWorker(service, new StoredWorkSignals(rabbit), Duration.ofHours(1));

    @Test
    void listenerRunsTheRequestedSynchronizationWithTheStaleThreshold() {
        var before = Instant.now();

        worker.listen(7);

        verify(service).syncRequestedStorageProvider(eq(7), argThat(staleBefore ->
                !staleBefore.isBefore(before.minus(Duration.ofHours(1)))
                        && staleBefore.isBefore(Instant.now().minus(Duration.ofMinutes(59)))));
        verifyNoInteractions(rabbit);
    }

    @Test
    void listenerPublishesANewerRequestThatSupersededTheRun() {
        when(service.syncRequestedStorageProvider(eq(7), any())).thenReturn(true);

        worker.listen(7);

        verify(rabbit).convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, 7);
    }

    @Test
    void failedSynchronizationIsNotHandedBackToTheBroker() {
        doThrow(new QueryTimeoutException("database busy")).when(service).syncRequestedStorageProvider(eq(7), any());

        assertDoesNotThrow(() -> worker.listen(7));
    }

    @Test
    void unavailableBrokerDoesNotFailTheStoredRequest() {
        doThrow(new AmqpConnectException(new IllegalStateException("broker unavailable"))).when(rabbit)
                .convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, 7);

        assertDoesNotThrow(() -> worker.publish(7));
    }
}
