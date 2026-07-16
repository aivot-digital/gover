package de.aivot.gover.backend.storage.services;

import de.aivot.gover.backend.asset.repositories.AssetRepository;
import de.aivot.gover.backend.storage.entities.StorageIndexItemEntity;
import de.aivot.gover.backend.storage.entities.StorageIndexItemEntityId;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import de.aivot.gover.backend.storage.models.StorageDocument;
import de.aivot.gover.backend.storage.models.StorageFolder;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.models.StorageProviderDefinition;
import de.aivot.gover.backend.storage.repositories.StorageIndexItemRepository;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageSyncServiceTest {
    @Mock
    private KnownExtensionsService knownExtensions;

    @Mock
    private StorageProviderRepository storageProviderRepository;

    @Mock
    private StorageProviderDefinitionService storageProviderDefinitionService;

    @Mock
    private StorageProviderConfigurationService storageProviderConfigurationService;

    @Mock
    private StorageIndexItemRepository storageIndexItemRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private StorageProviderDefinition<Object> storageProviderDefinition;

    private StorageSyncService storageSyncService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storageSyncService = new StorageSyncService(
                knownExtensions,
                storageProviderRepository,
                storageProviderDefinitionService,
                storageProviderConfigurationService,
                storageIndexItemRepository,
                assetRepository
        );
    }

    @Test
    void syncStorageProviderUpdatesIndexItemTimestampsFromProvider() throws Exception {
        var provider = new StorageProviderEntity()
                .setId(7)
                .setName("Provider")
                .setStorageProviderDefinitionKey("test")
                .setStorageProviderDefinitionVersion(1)
                .setType(StorageProviderType.Attachments)
                .setMetadataAttributes(List.of());
        var config = new Object();
        var remoteCreated = Instant.parse("2026-01-02T03:04:05Z");
        var remoteUpdated = Instant.parse("2026-01-03T04:05:06Z");
        var existingCreated = Instant.parse("2025-01-02T03:04:05Z");
        var existingUpdated = Instant.parse("2025-01-03T04:05:06Z");

        var document = new StorageDocument("/file.txt", "file.txt", 5L, StorageItemMetadata.empty());
        document
                .setCreated(remoteCreated)
                .setUpdated(remoteUpdated);
        var root = new StorageFolder("/", "Root", new LinkedList<>(), new LinkedList<>(), true)
                .addDocument(document);

        var existingRoot = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                "/",
                true,
                "Root",
                0L,
                StorageService.FOLDER_MIME_TYPE,
                false,
                StorageItemMetadata.empty(),
                existingCreated,
                existingUpdated
        );
        var existingDocument = new StorageIndexItemEntity(
                provider.getId(),
                provider.getType(),
                "/file.txt",
                false,
                "file.txt",
                5L,
                "text/plain",
                false,
                StorageItemMetadata.empty(),
                existingCreated,
                existingUpdated
        );

        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenReturn(Optional.of(storageProviderDefinition));
        when(storageProviderConfigurationService.mapToConfig(provider, storageProviderDefinition)).thenReturn(config);
        when(storageProviderDefinition.rootFolder(config, true)).thenReturn(root);
        when(storageProviderDefinition.getSupportsMetadataAttributes()).thenReturn(false);
        when(knownExtensions.determineMimeType("file.txt")).thenReturn(Optional.of("text/plain"));
        when(storageIndexItemRepository.findById(any())).thenAnswer(invocation -> {
            var id = invocation.getArgument(0, StorageIndexItemEntityId.class);
            if ("/".equals(id.getPathFromRoot())) {
                return Optional.of(existingRoot);
            }
            if ("/file.txt".equals(id.getPathFromRoot())) {
                return Optional.of(existingDocument);
            }
            return Optional.empty();
        });
        when(storageIndexItemRepository.findAllByStorageProviderId(provider.getId())).thenReturn(List.of(existingRoot, existingDocument));

        storageSyncService.syncStorageProvider(provider);

        var indexItemCaptor = ArgumentCaptor.forClass(StorageIndexItemEntity.class);
        verify(storageIndexItemRepository).save(indexItemCaptor.capture());

        var savedItem = indexItemCaptor.getValue();
        assertEquals("/file.txt", savedItem.getPathFromRoot());
        assertEquals(remoteCreated, savedItem.getCreated());
        assertEquals(remoteUpdated, savedItem.getUpdated());
    }
}
