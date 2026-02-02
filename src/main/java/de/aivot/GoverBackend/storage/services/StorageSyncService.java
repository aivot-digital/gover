package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.elements.exceptions.ElementDataConversionException;
import de.aivot.GoverBackend.elements.utils.ElementPOJOMapper;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class StorageSyncService {
    private static final Logger logger = LoggerFactory.getLogger(StorageSyncService.class);

    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageIndexItemRepository storageIndexItemRepository;

    public StorageSyncService(StorageProviderRepository storageProviderRepository, StorageProviderDefinitionService storageProviderDefinitionService, StorageIndexItemRepository storageIndexItemRepository) {
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageIndexItemRepository = storageIndexItemRepository;
    }

    public void syncStorageProvider(int id) {
        var storageProvider = storageProviderRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Storage provider with ID " + id + " not found."));
        syncStorageProvider(storageProvider);
    }

    public void syncStorageProvider(@Nonnull StorageProviderEntity storageProvider) {
        storageProviderRepository
                .save(storageProvider
                        .setStatus(StorageProviderStatus.Syncing)
                        .setStatusMessage(null)
                );

        try {
            performSync(storageProvider);
            storageProvider
                    .setStatus(StorageProviderStatus.Synced)
                    .setStatusMessage(null);
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Error syncing storage provider {} ({}): {}")
                    .addArgument(storageProvider::getName)
                    .addArgument(storageProvider::getId)
                    .addArgument(e::getMessage)
                    .log();

            storageProvider
                    .setStatus(StorageProviderStatus.SyncFailed)
                    .setStatusMessage(e.getMessage());
        } finally {
            storageProviderRepository.save(storageProvider);
        }
    }

    private void performSync(@Nonnull StorageProviderEntity storageProvider) {
        logger
                .atInfo()
                .setMessage("Syncing storage provider {} ({})")
                .addArgument(storageProvider::getName)
                .addArgument(storageProvider::getId)
                .log();

        var storageDefinition = storageProviderDefinitionService
                .retrieveProviderDefinition(storageProvider.getStorageProviderDefinitionKey(), storageProvider.getStorageProviderDefinitionVersion())
                .orElseThrow(() -> new IllegalStateException(
                        "Die Definition für den Speicheranbieter mit dem Schlüssel " +
                                storageProvider.getStorageProviderDefinitionKey() +
                                " und der Version " +
                                storageProvider.getStorageProviderDefinitionVersion() +
                                " konnte nicht gefunden werden. Bitte stellen Sie sicher, dass das entsprechende Plugin installiert ist und Sie die Anwendung neu gestartet haben."
                ));

        var root = getRoot(storageProvider, storageDefinition);

        Set<String> syncedPaths = new HashSet<>();

        root.apply(
                folder -> {
                    logger
                            .atInfo()
                            .setMessage("Storage provider '{}' root folder synced at path '{}'")
                            .addArgument(storageProvider::getName)
                            .addArgument(folder::getPathFromRoot)
                            .log();

                    var folderExists = storageIndexItemRepository
                            .existsById(StorageIndexItemEntityId.of(storageProvider.getId(), folder.getPathFromRoot()));

                    if (!folderExists) {
                        var item = new StorageIndexItemEntity(
                                storageProvider.getId(),
                                storageProvider.getType(),
                                folder.getPathFromRoot(),
                                true,
                                folder.getName(),
                                "inode/directory"
                        );
                        storageIndexItemRepository.save(item);
                    }

                    syncedPaths.add(folder.getPathFromRoot());

                    for (var document : folder.getDocuments()) {
                        var documentExists = storageIndexItemRepository
                                .existsById(StorageIndexItemEntityId.of(storageProvider.getId(), document.pathFromRoot()));

                        if (!documentExists) {
                            var mimeType = MediaTypeFactory
                                    .getMediaType(document.filename())
                                    .orElse(MediaType.APPLICATION_OCTET_STREAM)
                                    .getType();

                            var item = new StorageIndexItemEntity(
                                    storageProvider.getId(),
                                    storageProvider.getType(),
                                    document.pathFromRoot(),
                                    false,
                                    document.filename(),
                                    mimeType
                            );
                            storageIndexItemRepository.save(item);

                            syncedPaths.add(document.pathFromRoot());
                        }
                    }
                }
        );

        // Cleanup old entries
        var allItems = storageIndexItemRepository.findAllByStorageProviderId(storageProvider.getId());
        for (var item : allItems) {
            if (!syncedPaths.contains(item.getPathFromRoot())) {
                logger
                        .atInfo()
                        .setMessage("Removing stale storage index item at path '{}' for storage provider '{}' ({})")
                        .addArgument(item::getPathFromRoot)
                        .addArgument(storageProvider::getName)
                        .addArgument(storageProvider::getId)
                        .log();

                storageIndexItemRepository.delete(item);
            }
        }
    }

    private static <T> StorageFolder getRoot(StorageProviderEntity e, StorageProviderDefinition<T> def) {
        // Try to map the configuration to POJO for later usage
        T config;
        try {
            config = ElementPOJOMapper
                    .mapToPOJO(e.getConfiguration(), def.getConfigClass());
        } catch (ElementDataConversionException ex) {
            throw new IllegalStateException(
                    "Die Konfiguration des Speicheranbieters " +
                            e.getName() +
                            " mit dem Schlüssel " +
                            e.getStorageProviderDefinitionKey() +
                            " und der Version " +
                            e.getStorageProviderDefinitionVersion() +
                            " für die Speicheranbieter-Definition konnte nicht verarbeitet werden. Bitte stellen Sie sicher, dass der Speicheranbieter korrekt konfiguriert ist.", ex);
        }

        // Initialize provider for usage
        def.initializeProvider(config);

        // Retrieve root folder recursively
        return def.rootFolder(config, true);
    }
}
