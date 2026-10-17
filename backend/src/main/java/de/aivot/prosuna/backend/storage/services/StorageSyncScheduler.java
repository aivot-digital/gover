package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class StorageSyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(StorageSyncScheduler.class);

    private final StorageProviderRepository storageProviderRepository;
    private final StorageSyncWorker storageSyncWorker;
    private final StoredWorkSignals signals;
    private final Duration sweepInterval;
    private final Duration staleThreshold;

    public StorageSyncScheduler(StorageProviderRepository storageProviderRepository,
                                StorageSyncWorker storageSyncWorker,
                                StoredWorkSignals signals,
                                @Value("${prosuna.background-sync.sweep-interval:PT5M}") Duration sweepInterval,
                                @Value("${prosuna.background-sync.stale-threshold:PT1H}") Duration staleThreshold) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageSyncWorker = storageSyncWorker;
        this.signals = signals;
        this.sweepInterval = sweepInterval;
        this.staleThreshold = staleThreshold;
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "${prosuna.timezone}")
    public void queueNightlyStorageSync() {
        requestSyncOfAll();
    }

    /** Requests a synchronization of all providers. */
    public void requestSyncOfAll() {
        // The requests are stored first, so a lost message is published again by the sweep.
        storageProviderRepository.requestSyncOfAll(Instant.now());
        var storageProviders = storageProviderRepository.findAll();

        logger
                .atInfo()
                .setMessage("Queueing storage sync for {} storage provider(s).")
                .addArgument(storageProviders::size)
                .log();

        for (var storageProvider : storageProviders) {
            storageSyncWorker.publish(storageProvider.getId());
        }
    }

    @Scheduled(fixedDelayString = "${prosuna.background-sync.sweep-interval:PT5M}")
    public void republishPendingStorageSyncs() {
        var now = Instant.now();
        var requestedBefore = now.minus(sweepInterval);
        signals.republish(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE, storageProviderRepository.findPendingSyncIds(requestedBefore),
                storageProviderId -> storageProviderRepository.claimSyncRedispatch(storageProviderId, requestedBefore, now) == 1);
        // Interrupted runs are taken over by the consumer's claim. The sweep only reads them, so it never waits for
        // the lock of a run that is still recording its result.
        signals.republish(StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE,
                storageProviderRepository.findInterruptedSyncIds(now.minus(staleThreshold)), storageProviderId -> true);
    }
}
