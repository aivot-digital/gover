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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
