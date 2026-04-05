package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntityId;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderStatus;
import de.aivot.GoverBackend.storage.exceptions.StorageException;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageFolder;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageSyncService {
    private static final Logger logger = LoggerFactory.getLogger(StorageSyncService.class);
    private static final String SYSTEM_SYNC_UPLOADER_ID = null;

    private final KnownExtensionsService knownExtensions;
    private final StorageProviderRepository storageProviderRepository;
    private final StorageProviderDefinitionService storageProviderDefinitionService;
    private final StorageProviderConfigurationService storageProviderConfigurationService;
    private final StorageIndexItemRepository storageIndexItemRepository;
    private final AssetRepository assetRepository;

    public StorageSyncService(KnownExtensionsService knownExtensions,
                              StorageProviderRepository storageProviderRepository,
                              StorageProviderDefinitionService storageProviderDefinitionService,
                              StorageProviderConfigurationService storageProviderConfigurationService,
                              StorageIndexItemRepository storageIndexItemRepository,
                              AssetRepository assetRepository) {
        this.knownExtensions = knownExtensions;
        this.storageProviderRepository = storageProviderRepository;
        this.storageProviderDefinitionService = storageProviderDefinitionService;
        this.storageProviderConfigurationService = storageProviderConfigurationService;
        this.storageIndexItemRepository = storageIndexItemRepository;
        this.assetRepository = assetRepository;
    }

    public void syncStorageProvider(int id) {
        var storageProviderOpt = storageProviderRepository
                .findById(id);

        if (storageProviderOpt.isEmpty()) {
            logger
                    .atError()
                    .setMessage("Storage provider with ID {} not found for syncing")
                    .addArgument(id)
                    .log();
            return;
        }

        var storageProvider = storageProviderOpt.get();

        try {
            syncStorageProvider(storageProvider);
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Error syncing storage provider with ID {}: {}")
                    .addArgument(id)
                    .addArgument(e::getMessage)
                    .setCause(e)
                    .log();
        }
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
                    .setStatusMessage(null)
                    .setLastSync(LocalDateTime.now());
        } catch (Exception e) {
            logger
                    .atError()
                    .setMessage("Error syncing storage provider {} ({}): {}")
                    .addArgument(storageProvider::getName)
                    .addArgument(storageProvider::getId)
                    .addArgument(e::getMessage)
                    .setCause(e)
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
        var supportsMetadataAttributes = storageDefinition.getSupportsMetadataAttributes();

        Set<String> syncedPaths = new HashSet<>();

        root.apply(
                folder -> {
                    logger
                            .atInfo()
                            .setMessage("Storage provider '{}' root folder synced at path '{}'")
                            .addArgument(storageProvider::getName)
                            .addArgument(folder::getPathFromRoot)
                            .log();

                    var folderItem = storageIndexItemRepository
                            .findById(StorageIndexItemEntityId.of(storageProvider.getId(), folder.getPathFromRoot()))
                            .orElse(null);

                    if (folderItem == null) {
                        folderItem = new StorageIndexItemEntity(
                                storageProvider.getId(),
                                storageProvider.getType(),
                                folder.getPathFromRoot(),
                                true,
                                folder.getName(),
                                0L,
                                StorageService.FOLDER_MIME_TYPE,
                                false,
                                StorageItemMetadata.empty(), // Only store metadata for documents, not for folders for now
                                LocalDateTime.now(),
                                LocalDateTime.now()
                        );
                    }

                    folderItem
                            .setStorageProviderType(storageProvider.getType())
                            .setMimeType(StorageService.FOLDER_MIME_TYPE)
                            .setDirectory(true)
                            .setFilename(folder.getName())
                            .setSizeInBytes(0L)
                            .setMissing(false)
                            .setMetadata(StorageItemMetadata.empty());

                    storageIndexItemRepository.save(folderItem);

                    syncedPaths.add(folder.getPathFromRoot());

                    for (var document : folder.getDocuments()) {
                        var filteredDocumentMetadata = supportsMetadataAttributes
                                ? filterMetadataByRegisteredAttributes(storageProvider, document.getMetadata())
                                : StorageItemMetadata.empty();

                        var docItem = storageIndexItemRepository
                                .findById(StorageIndexItemEntityId.of(storageProvider.getId(), document.getPathFromRoot()))
                                .orElse(null);

                        var mimeType = knownExtensions
                                .determineMimeType(document.getName())
                                .orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

                        if (docItem == null) {
                            docItem = new StorageIndexItemEntity(
                                    storageProvider.getId(),
                                    storageProvider.getType(),
                                    document.getPathFromRoot(),
                                    false,
                                    document.getName(),
                                    document.getSizeInBytes(),
                                    mimeType,
                                    false,
                                    filteredDocumentMetadata,
                                    LocalDateTime.now(),
                                    LocalDateTime.now()
                            );
                            storageIndexItemRepository.save(docItem);
                        } else if (hasDocumentIndexItemChanged(docItem, storageProvider, document, mimeType, filteredDocumentMetadata)) {
                            docItem
                                    .setStorageProviderType(storageProvider.getType())
                                    .setMimeType(mimeType)
                                    .setDirectory(false)
                                    .setFilename(document.getName())
                                    .setSizeInBytes(document.getSizeInBytes())
                                    .setMissing(false)
                                    .setMetadata(filteredDocumentMetadata)
                                    .setUpdated(LocalDateTime.now());

                            storageIndexItemRepository.save(docItem);
                        }

                        syncAssetEntityForDocument(storageProvider, docItem);

                        syncedPaths.add(docItem.getPathFromRoot());
                    }
                }
        );

        // Mark old entries as missing
        var allItems = storageIndexItemRepository.
                findAllByStorageProviderId(storageProvider.getId());

        for (var item : allItems) {
            if (!syncedPaths.contains(item.getPathFromRoot())) {
                logger
                        .atInfo()
                        .setMessage("Removing stale storage index item at path '{}' for storage provider '{}' ({})")
                        .addArgument(item::getPathFromRoot)
                        .addArgument(storageProvider::getName)
                        .addArgument(storageProvider::getId)
                        .log();

                storageIndexItemRepository.save(item
                        .setMissing(true)
                        .setUpdated(LocalDateTime.now()));

                removeAssetEntityForMissingDocument(storageProvider, item);
            }
        }
    }

    private <T> StorageFolder getRoot(StorageProviderEntity e, StorageProviderDefinition<T> def) {
        T config;
        try {
            config = storageProviderConfigurationService
                    .mapToConfig(e, def);
        } catch (ResponseException ex) {
            throw new IllegalStateException(
                    "Die Konfiguration des Speicheranbieters " +
                            e.getName() +
                            " mit dem Schlüssel " +
                            e.getStorageProviderDefinitionKey() +
                            " und der Version " +
                            e.getStorageProviderDefinitionVersion() +
                            " für die Speicheranbieter-Definition konnte nicht verarbeitet werden. Bitte stellen Sie sicher, dass der Speicheranbieter korrekt konfiguriert ist.", ex);
        }

        try {
            // Initialize provider for usage
            def.initializeProvider(config);

            // Retrieve root folder recursively
            return def.rootFolder(config, true);
        } catch (StorageException ex) {
            throw new IllegalStateException(
                    "Der Speicheranbieter " +
                            e.getName() +
                            " (" + e.getId() + ") konnte nicht synchronisiert werden, da auf den Speicher nicht zugegriffen werden konnte.",
                    ex
            );
        }
    }

    private static StorageItemMetadata filterMetadataByRegisteredAttributes(@Nonnull StorageProviderEntity provider,
                                                                            @Nonnull StorageItemMetadata metadata) {
        var filteredMetadata = new StorageItemMetadata();

        for (var metadataAttribute : provider.getMetadataAttributes()) {
            var key = metadataAttribute.getKey();
            if (metadata.containsKey(key)) {
                filteredMetadata.put(key, metadata.get(key));
            }
        }

        return filteredMetadata;
    }

    private static boolean hasDocumentIndexItemChanged(@Nonnull StorageIndexItemEntity existingItem,
                                                       @Nonnull StorageProviderEntity storageProvider,
                                                       @Nonnull StorageDocument document,
                                                       @Nonnull String mimeType,
                                                       @Nonnull StorageItemMetadata filteredDocumentMetadata) {
        return !Objects.equals(existingItem.getStorageProviderType(), storageProvider.getType())
                || !Objects.equals(existingItem.getMimeType(), mimeType)
                || !Objects.equals(existingItem.getDirectory(), false)
                || !Objects.equals(existingItem.getFilename(), document.getName())
                || !Objects.equals(existingItem.getSizeInBytes(), document.getSizeInBytes())
                || !Objects.equals(existingItem.getMissing(), false)
                || !Objects.equals(existingItem.getMetadata(), filteredDocumentMetadata);
    }

    private void syncAssetEntityForDocument(@Nonnull StorageProviderEntity storageProvider,
                                            @Nonnull StorageIndexItemEntity documentIndexItem) {
        if (storageProvider.getType() != StorageProviderType.Assets) {
            return;
        }

        var asset = assetRepository
                .findByStorageProviderIdAndStoragePathFromRoot(storageProvider.getId(), documentIndexItem.getPathFromRoot())
                .orElseGet(() -> new AssetEntity()
                        .setKey(UUID.randomUUID())
                        .setUploaderId(SYSTEM_SYNC_UPLOADER_ID)
                        .setPrivate(true)
                        .setStorageProviderId(storageProvider.getId())
                        .setStoragePathFromRoot(documentIndexItem.getPathFromRoot()));

        asset.setStorageProviderId(storageProvider.getId());
        asset.setStoragePathFromRoot(documentIndexItem.getPathFromRoot());

        assetRepository.save(asset);
    }

    private void removeAssetEntityForMissingDocument(@Nonnull StorageProviderEntity storageProvider,
                                                     @Nonnull StorageIndexItemEntity indexItem) {
        if (storageProvider.getType() != StorageProviderType.Assets || Boolean.TRUE.equals(indexItem.getDirectory())) {
            return;
        }

        assetRepository
                .findByStorageProviderIdAndStoragePathFromRoot(storageProvider.getId(), indexItem.getPathFromRoot())
                .ifPresent(assetRepository::delete);
    }
}
