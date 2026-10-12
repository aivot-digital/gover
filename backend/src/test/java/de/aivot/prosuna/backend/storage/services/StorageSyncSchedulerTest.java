package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.storage.entities.StorageProviderEntity;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class StorageSyncSchedulerTest {
    private final StorageProviderRepository providers = mock(StorageProviderRepository.class);
    private final StorageSyncWorker worker = mock(StorageSyncWorker.class);
    private final StorageSyncScheduler scheduler = new StorageSyncScheduler(providers, worker, Duration.ofMinutes(5));

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
    void sweepPublishesOnlyClaimedPendingRequests() {
        when(providers.findPendingSyncIds(any())).thenReturn(List.of(1, 2));
        when(providers.claimSyncRedispatch(eq(1), any(), any())).thenReturn(1);

        scheduler.republishPendingStorageSyncs();

        verify(worker).publish(1);
        verify(worker, never()).publish(2);
    }
}
