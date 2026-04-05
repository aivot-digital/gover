package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StorageSyncScheduler {
    private static final Logger logger = LoggerFactory.getLogger(StorageSyncScheduler.class);

    private final StorageProviderRepository storageProviderRepository;
    private final RabbitTemplate rabbitTemplate;

    public StorageSyncScheduler(StorageProviderRepository storageProviderRepository,
                                RabbitTemplate rabbitTemplate) {
        this.storageProviderRepository = storageProviderRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "Europe/Paris")
    public void queueNightlyStorageSync() {
        var storageProviders = storageProviderRepository.findAll();

        logger
                .atInfo()
                .setMessage("Queueing nightly storage sync for {} storage provider(s).")
                .addArgument(storageProviders::size)
                .log();

        for (var storageProvider : storageProviders) {
            rabbitTemplate.convertAndSend(
                    StorageSyncWorker.DO_WORK_ON_STORAGE_SYNC_QUEUE,
                    storageProvider.getId()
            );
        }
    }
}
