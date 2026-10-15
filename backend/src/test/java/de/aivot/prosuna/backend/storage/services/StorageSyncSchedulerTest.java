package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StorageSyncSchedulerTest {
    private final StorageProviderRepository providers = mock(StorageProviderRepository.class);
    private final StorageSyncWorker worker = mock(StorageSyncWorker.class);
    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final StorageSyncScheduler scheduler = new StorageSyncScheduler(providers, worker, new StoredWorkSignals(rabbit),
            Duration.ofMinutes(5), Duration.ofHours(1));

    @Test
    void nightlySyncStoresTheRequestsBeforePublishingThem() {
        when(providers.findAll()).thenReturn(List.of(new StorageProviderEntity().setId(1), new StorageProviderEntity().setId(2)));

        scheduler.queueNightlyStorageSync();

        var order = inOrder(providers, worker);
        order.verify(providers).requestSyncOfAll(any());
        order.verify(worker).publish(1);
        order.verify(worker).publish(2);
    }

    @Test
    void sweepPublishesClaimedPendingRequestsAndInterruptedRuns() {
        when(providers.findPendingSyncIds(any())).thenReturn(List.of(1, 2));
        when(providers.claimSyncRedispatch(eq(1), any(), any())).thenReturn(1);
        when(providers.findInterruptedSyncIds(any())).thenReturn(List.of(3));
        var before = Instant.now();

        scheduler.republishPendingStorageSyncs();

        verify(providers).findInterruptedSyncIds(argThat(startedBefore ->
                !startedBefore.isBefore(before.minus(Duration.ofHours(1)))
                        && startedBefore.isBefore(Instant.now().minus(Duration.ofMinutes(59)))));
        verify(rabbit).convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, 1);
        verify(rabbit, never()).convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, 2);
        verify(rabbit).convertAndSend(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, 3);
        verify(providers, never()).claimSyncRedispatch(eq(3), any(), any());
    }
}
