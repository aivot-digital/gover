package de.aivot.gover.backend.storage.services;

import de.aivot.gover.backend.TestData;
import de.aivot.gover.backend.asset.entities.AssetEntity;
import de.aivot.gover.backend.asset.repositories.AssetRepository;
import de.aivot.gover.backend.av.services.AVService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.storage.entities.StorageIndexItemEntity;
import de.aivot.gover.backend.storage.entities.StorageProviderEntity;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import de.aivot.gover.backend.storage.models.StorageDocument;
import de.aivot.gover.backend.storage.models.StorageItemMetadata;
import de.aivot.gover.backend.storage.models.StorageProviderDefinition;
import de.aivot.gover.backend.storage.repositories.StorageIndexItemRepository;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private AssetRepository assetRepository;

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
    void storeDocumentRejectsPercentEncodedTraversalBeforeProviderLookup() {
        var exception = assertThrows(
                ResponseException.class,
                () -> storageService.storeDocument(
                        1,
                        "/folder/%2e%2e/file.txt",
                        new ByteArrayInputStream(new byte[0]),
                        StorageItemMetadata.empty()
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verifyNoInteractions(
                storageProviderRepository,
                storageProviderDefinitionService,
                storageProviderConfigurationService,
                storageProviderDefinition,
                storageIndexItemRepository,
                assetRepository,
                avService
        );
    }

    @Test
    void storeDocumentNormalizesSafePercentEscapesBeforeProviderCall() throws Exception {
        var provider = createWritableProvider();
        var config = new Object();
        var storedDocument = new StorageDocument(
                "/folder/file name+.txt",
                "file name+.txt",
                4L,
                StorageItemMetadata.empty()
        );

        when(storageProviderRepository.findById(1)).thenReturn(Optional.of(provider));
        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenReturn(Optional.of(storageProviderDefinition));
        when(storageProviderConfigurationService.mapToConfig(provider, storageProviderDefinition)).thenReturn(config);
        when(storageProviderDefinition.getSupportsMetadataAttributes()).thenReturn(false);
        when(storageProviderDefinition.storeDocument(
                eq(config),
                eq("/folder/file name+.txt"),
                any(InputStream.class),
                eq(StorageItemMetadata.empty())
        )).thenReturn(storedDocument);
        when(knownExtensionsService.determineMimeType("file name+.txt")).thenReturn(Optional.of("text/plain"));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(1, "/folder/file name+.txt"))
                .thenReturn(Optional.empty());

        storageService.storeDocument(
                1,
                "folder/file%20name+.txt",
                new ByteArrayInputStream("data".getBytes()),
                StorageItemMetadata.empty()
        );

        verify(avService).testFile(any(InputStream.class), eq("/folder/file name+.txt"));
        verify(storageProviderDefinition).storeDocument(
                eq(config),
                eq("/folder/file name+.txt"),
                any(InputStream.class),
                eq(StorageItemMetadata.empty())
        );
    }

    @Test
    void storeDocument_CreatesPrivateAssetForAssetProvider() throws Exception {
        var provider = createWritableProvider();
        var config = new Object();
        var storedDocument = new StorageDocument(
                "/folder/new.pdf",
                "new.pdf",
                4L,
                StorageItemMetadata.empty()
        );

        when(storageProviderRepository.findById(1)).thenReturn(Optional.of(provider));
        when(storageProviderDefinitionService.retrieveProviderDefinition("test", 1)).thenReturn(Optional.of(storageProviderDefinition));
        when(storageProviderConfigurationService.mapToConfig(provider, storageProviderDefinition)).thenReturn(config);
        when(storageProviderDefinition.getSupportsMetadataAttributes()).thenReturn(false);
        when(storageProviderDefinition.storeDocument(
                eq(config),
                eq("/folder/new.pdf"),
                any(InputStream.class),
                eq(StorageItemMetadata.empty())
        )).thenReturn(storedDocument);
        when(knownExtensionsService.determineMimeType("new.pdf")).thenReturn(Optional.of("application/pdf"));
        when(assetRepository.findByStorageProviderIdAndStoragePathFromRoot(1, "/folder/new.pdf"))
                .thenReturn(Optional.empty());

        storageService.storeDocument(
                1,
                "/folder/new.pdf",
                new ByteArrayInputStream("data".getBytes()),
                StorageItemMetadata.empty()
        );

        var assetCaptor = ArgumentCaptor.forClass(AssetEntity.class);
        verify(assetRepository).save(assetCaptor.capture());

        var savedAsset = assetCaptor.getValue();
        assertEquals(1, savedAsset.getStorageProviderId());
        assertEquals("/folder/new.pdf", savedAsset.getStoragePathFromRoot());
        assertEquals(true, savedAsset.getPrivate());
        assertNull(savedAsset.getUploaderId());
    }

    @Test
    void moveDocument_UpdatesMimeTypeBasedOnTargetExtension() throws Exception {
        var provider = createWritableProvider();

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

    private static StorageProviderEntity createWritableProvider() {
        return new StorageProviderEntity()
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
    }

    @Test
    void searchIndexItems_BlankSearch_ReturnsEmptyPageWithoutQueryingRepository() {
        var result = storageService.searchIndexItems(3, "   ", false, PageRequest.of(1, 5));

        assertEquals(0, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());

        verifyNoInteractions(storageIndexItemRepository);
    }

    @Test
    void searchIndexItems_WithoutExplicitSort_UsesDefaultOrdering() {
        when(storageIndexItemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<StorageIndexItemEntity>>any(), org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(Page.empty());

        storageService.searchIndexItems(3, "readme", false, PageRequest.of(2, 25));

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(storageIndexItemRepository).findAll(org.mockito.ArgumentMatchers.<Specification<StorageIndexItemEntity>>any(), pageableCaptor.capture());

        var effectivePageable = pageableCaptor.getValue();
        assertEquals(2, effectivePageable.getPageNumber());
        assertEquals(25, effectivePageable.getPageSize());
        assertEquals(Sort.Direction.DESC, effectivePageable.getSort().getOrderFor("directory").getDirection());
        assertEquals(Sort.Direction.ASC, effectivePageable.getSort().getOrderFor("filename").getDirection());
        assertEquals(Sort.Direction.ASC, effectivePageable.getSort().getOrderFor("pathFromRoot").getDirection());
    }

    @Test
    void searchIndexItems_FiltersDisallowedSortProperties() {
        when(storageIndexItemRepository.findAll(org.mockito.ArgumentMatchers.<Specification<StorageIndexItemEntity>>any(), org.mockito.ArgumentMatchers.<Pageable>any()))
                .thenReturn(Page.empty());

        storageService.searchIndexItems(
                3,
                "readme",
                false,
                PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Order.desc("storageProviderId"),
                                Sort.Order.asc("filename"),
                                Sort.Order.desc("updated")
                        )
                )
        );

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(storageIndexItemRepository).findAll(org.mockito.ArgumentMatchers.<Specification<StorageIndexItemEntity>>any(), pageableCaptor.capture());

        var effectivePageable = pageableCaptor.getValue();
        assertNull(effectivePageable.getSort().getOrderFor("storageProviderId"));
        assertEquals(Sort.Direction.ASC, effectivePageable.getSort().getOrderFor("filename").getDirection());
        assertEquals(Sort.Direction.DESC, effectivePageable.getSort().getOrderFor("updated").getDirection());
    }
}
