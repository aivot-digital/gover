package de.aivot.GoverBackend.storage.services;

import de.aivot.GoverBackend.TestData;
import de.aivot.GoverBackend.av.services.AVService;
import de.aivot.GoverBackend.storage.entities.StorageIndexItemEntity;
import de.aivot.GoverBackend.storage.entities.StorageProviderEntity;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.models.StorageDocument;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.models.StorageProviderDefinition;
import de.aivot.GoverBackend.storage.repositories.StorageIndexItemRepository;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageServiceTest {
    @Mock
    private StorageProviderRepository storageProviderRepository;

    @Mock
    private StorageProviderDefinitionService storageProviderDefinitionService;

    @Mock
    private StorageProviderConfigurationService storageProviderConfigurationService;

    @Mock
    private StorageIndexItemRepository storageIndexItemRepository;

    @Mock
    private KnownExtensionsService knownExtensionsService;

    @Mock
    private AVService avService;

    @Mock
    private StorageProviderDefinition<Object> storageProviderDefinition;

    @InjectMocks
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void moveDocument_UpdatesMimeTypeBasedOnTargetExtension() throws Exception {
        var provider = new StorageProviderEntity()
                .setId(1)
                .setName("Test Provider")
                .setDescription("Test Provider")
                .setStorageProviderDefinitionKey("test")
                .setStorageProviderDefinitionVersion(1)
                .setType(StorageProviderType.Assets)
                .setReadOnlyStorage(false)
                .setTestProvider(false)
                .setSystemProvider(false)
                .setMaxFileSizeInBytes(0L)
                .setMetadataAttributes(List.of())
                .setConfiguration(TestData.authored());

        var config = new Object();
        var movedDocument = new StorageDocument(
                "/folder/test.md",
                "test.md",
                42L,
                StorageItemMetadata.empty()
        );

        when(storageProviderRepository.findById(1)).thenReturn(Optional.of(provider));
        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenReturn(Optional.of(storageProviderDefinition));
        when(storageProviderConfigurationService.mapToConfig(provider, storageProviderDefinition)).thenReturn(config);
        when(storageProviderDefinition.moveDocument(config, "/folder/test.txt", "/folder/test.md")).thenReturn(movedDocument);
        when(knownExtensionsService.determineMimeType("test.md")).thenReturn(Optional.of("text/markdown"));

        storageService.moveDocument(1, "/folder/test.txt", "/folder/test.md");

        verify(storageIndexItemRepository).moveDocumentPath(1, "/folder/test.txt", "/folder/test.md");

        var indexItemCaptor = ArgumentCaptor.forClass(StorageIndexItemEntity.class);
        verify(storageIndexItemRepository).save(indexItemCaptor.capture());

        var savedIndexItem = indexItemCaptor.getValue();
        assertEquals("/folder/test.md", savedIndexItem.getPathFromRoot());
        assertEquals("test.md", savedIndexItem.getFilename());
        assertEquals("text/markdown", savedIndexItem.getMimeType());

        verify(knownExtensionsService).determineMimeType("test.md");
    }
}
