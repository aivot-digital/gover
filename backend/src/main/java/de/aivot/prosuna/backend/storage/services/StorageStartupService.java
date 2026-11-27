package de.aivot.prosuna.backend.storage.services;

import jakarta.annotation.Nonnull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StorageStartupService implements ApplicationListener<ApplicationReadyEvent> {
    private final StorageSyncScheduler storageSyncScheduler;

    public StorageStartupService(StorageSyncScheduler storageSyncScheduler) {
        this.storageSyncScheduler = storageSyncScheduler;
    }

    @Override
    public void onApplicationEvent(@Nonnull ApplicationReadyEvent event) {
        // Synchronizations run as stored requests, so another application instance cannot run the same one at once.
        storageSyncScheduler.requestSyncOfAll();
    }
}
