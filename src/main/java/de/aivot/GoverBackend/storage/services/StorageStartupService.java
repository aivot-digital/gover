package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StorageStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final StorageProviderRepository storageProviderRepository;
    private final StorageSyncService storageSyncService;

    public StorageStartupService(StorageProviderRepository storageProviderRepository,
                                 StorageSyncService storageSyncService) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageSyncService = storageSyncService;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        var allProviders = storageProviderRepository
                .findAll();

        for (var provider : allProviders) {
            storageSyncService.syncStorageProvider(provider);
        }
    }
}
