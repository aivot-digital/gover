package de.aivot.prosuna.backend.storage.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

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
    private final RabbitTemplate rabbitTemplate;

    public StorageSyncWorker(StorageSyncService storageSyncService, RabbitTemplate rabbitTemplate) {
        this.storageSyncService = storageSyncService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Bean
    public Queue doWorkOnStorageSyncQueue() {
        return new Queue(DO_WORK_ON_STORAGE_SYNC_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_STORAGE_SYNC_QUEUE)
    public void listen(Integer storageProviderId) {
        try {
            storageSyncService.syncRequestedStorageProvider(storageProviderId);
        } catch (RuntimeException exception) {
            // The synchronization rolled back to the pending status, so it is published again later.
            logger.error("Could not synchronize storage provider {}", storageProviderId, exception);
        }
    }

    /** Publishes a stored synchronization request. If this fails, the request is published again later. */
    public void publish(int storageProviderId) {
        try {
            rabbitTemplate.convertAndSend(DO_WORK_ON_STORAGE_SYNC_QUEUE, storageProviderId);
        } catch (RuntimeException exception) {
            logger.warn("Could not publish the synchronization of storage provider {}", storageProviderId, exception);
        }
    }
}
