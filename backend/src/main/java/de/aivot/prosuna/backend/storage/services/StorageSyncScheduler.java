package de.aivot.prosuna.backend.storage.services;

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
    private final Duration sweepInterval;

    public StorageSyncScheduler(StorageProviderRepository storageProviderRepository,
                                StorageSyncWorker storageSyncWorker,
                                @Value("${prosuna.background-sync.sweep-interval:PT5M}") Duration sweepInterval) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageSyncWorker = storageSyncWorker;
        this.sweepInterval = sweepInterval;
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "${prosuna.timezone}")
    public void queueNightlyStorageSync() {
        // The requests are stored first, so a lost message is published again by the sweep.
        storageProviderRepository.requestSyncOfAll(Instant.now());
        var storageProviders = storageProviderRepository.findAll();

        logger
                .atInfo()
                .setMessage("Queueing nightly storage sync for {} storage provider(s).")
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
        for (var storageProviderId : storageProviderRepository.findPendingSyncIds(requestedBefore)) {
            // The claim keeps other application instances from publishing the same request again.
            if (storageProviderRepository.claimSyncRedispatch(storageProviderId, requestedBefore, now) == 1) {
                storageSyncWorker.publish(storageProviderId);
            }
        }
    }
}
