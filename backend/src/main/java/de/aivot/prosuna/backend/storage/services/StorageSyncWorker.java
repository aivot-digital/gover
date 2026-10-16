package de.aivot.prosuna.backend.storage.services;

import de.aivot.prosuna.backend.lib.services.StoredWorkSignals;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Runs storage provider synchronizations in the background. A request is stored as the provider's pending status;
 * RabbitMQ only carries the provider ID as a wake-up signal, and {@link StorageSyncScheduler} publishes requests
 * again whose messages may have been lost.
 */
@Service
public class StorageSyncWorker {
    public static final String DO_WORK_ON_STORAGE_SYNC_QUEUE = "do-work-on-storage-sync-queue";
    private static final Logger logger = LoggerFactory.getLogger(StorageSyncWorker.class);

    private final StorageSyncService storageSyncService;
    private final StoredWorkSignals signals;
    private final Duration staleThreshold;

    public StorageSyncWorker(StorageSyncService storageSyncService, StoredWorkSignals signals,
                             @Value("${prosuna.background-sync.stale-threshold:PT1H}") Duration staleThreshold) {
        this.storageSyncService = storageSyncService;
        this.signals = signals;
        this.staleThreshold = staleThreshold;
    }

    @Bean
    public Queue doWorkOnStorageSyncQueue() {
        return new Queue(DO_WORK_ON_STORAGE_SYNC_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_STORAGE_SYNC_QUEUE)
    public void listen(@Nonnull Integer storageProviderId) {
        try {
            if (storageSyncService.syncRequestedStorageProvider(storageProviderId, Instant.now().minus(staleThreshold))) {
                publish(storageProviderId);
            }
        } catch (RuntimeException exception) {
            // The request remains stored, so it is published again later.
            logger.error("Could not synchronize storage provider {}", storageProviderId, exception);
        }
    }

    /** Publishes a stored synchronization request. If this fails, the request is published again later. */
    public void publish(int storageProviderId) {
        signals.publish(DO_WORK_ON_STORAGE_SYNC_QUEUE, storageProviderId);
    }
}
