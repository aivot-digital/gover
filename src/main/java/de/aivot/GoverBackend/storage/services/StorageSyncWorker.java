package de.aivot.GoverBackend.storage.services;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class StorageSyncWorker {
    public static final String DO_WORK_ON_STORAGE_SYNC_QUEUE = "do-work-on-storage-sync-queue";
    private final StorageSyncService storageSyncService;

    public StorageSyncWorker(StorageSyncService storageSyncService) {
        this.storageSyncService = storageSyncService;
    }

    @Bean
    public Queue doWorkOnStorageSyncQueue() {
        return new Queue(DO_WORK_ON_STORAGE_SYNC_QUEUE, true);
    }

    @RabbitListener(queues = DO_WORK_ON_STORAGE_SYNC_QUEUE)
    public void listen(Integer storageProviderId) {
        storageSyncService
                .syncStorageProvider(storageProviderId);

    }
}
