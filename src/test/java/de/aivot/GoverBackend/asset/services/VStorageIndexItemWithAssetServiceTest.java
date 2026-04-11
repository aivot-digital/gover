package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VStorageIndexItemWithAssetServiceTest {
    @Mock
    private VStorageIndexItemWithAssetRepository repository;

    private VStorageIndexItemWithAssetService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new VStorageIndexItemWithAssetService(repository);
    }

    @Test
    void searchIndexItems_BlankSearch_ReturnsEmptyPageWithoutQueryingRepository() {
        var result = service.searchIndexItems(3, "   ", null, null, PageRequest.of(1, 5));

        assertEquals(0, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());

        verifyNoInteractions(repository);
    }

    @Test
    void searchIndexItems_WithoutExplicitSort_UsesDefaultOrdering() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.searchIndexItems(3, "readme", null, null, PageRequest.of(2, 25));

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());

        var effectivePageable = pageableCaptor.getValue();
        assertEquals(2, effectivePageable.getPageNumber());
        assertEquals(25, effectivePageable.getPageSize());
        assertEquals(Sort.Direction.DESC, effectivePageable.getSort().getOrderFor("directory").getDirection());
        assertEquals(Sort.Direction.ASC, effectivePageable.getSort().getOrderFor("filename").getDirection());
        assertEquals(Sort.Direction.ASC, effectivePageable.getSort().getOrderFor("pathFromRoot").getDirection());
    }

    @Test
    void searchIndexItems_FiltersDisallowedSortProperties() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.searchIndexItems(
                3,
                "readme",
                null,
                null,
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
        verify(repository).findAll(any(Specification.class), pageableCaptor.capture());

        var effectivePageable = pageableCaptor.getValue();
        assertNull(effectivePageable.getSort().getOrderFor("storageProviderId"));
        assertEquals(Sort.Direction.ASC, effectivePageable.getSort().getOrderFor("filename").getDirection());
        assertEquals(Sort.Direction.DESC, effectivePageable.getSort().getOrderFor("updated").getDirection());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void searchIndexItems_WithContentTypeFilter_AddsMimeTypePrefixPredicate() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.searchIndexItems(3, "Readme", java.util.List.of(" IMAGE/ "), null, PageRequest.of(0, 10));

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Specification<VStorageIndexItemWithAssetEntity> specification = specificationCaptor.getValue();

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        Path storageProviderIdPath = mock(Path.class);
        Path mimeTypePath = mock(Path.class);
        Path filenamePath = mock(Path.class);
        Path pathFromRootPath = mock(Path.class);
        Expression lowerMimeType = mock(Expression.class);
        Expression lowerFilename = mock(Expression.class);
        Expression lowerPathFromRoot = mock(Expression.class);
        Predicate providerPredicate = mock(Predicate.class);
        Predicate visibilityPredicate = mock(Predicate.class);
        Predicate contentTypePredicate = mock(Predicate.class);
        Predicate filenamePredicate = mock(Predicate.class);
        Predicate pathPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("storageProviderId")).thenReturn(storageProviderIdPath);
        when(root.get("mimeType")).thenReturn(mimeTypePath);
        when(root.get("filename")).thenReturn(filenamePath);
        when(root.get("pathFromRoot")).thenReturn(pathFromRootPath);

        when(storageProviderIdPath.as(Integer.class)).thenReturn(storageProviderIdPath);
        when(mimeTypePath.as(String.class)).thenReturn(mimeTypePath);
        when(filenamePath.as(String.class)).thenReturn(filenamePath);
        when(pathFromRootPath.as(String.class)).thenReturn(pathFromRootPath);

        when(builder.equal(storageProviderIdPath, 3)).thenReturn(providerPredicate);
        when(builder.conjunction()).thenReturn(visibilityPredicate);
        when(builder.lower(mimeTypePath)).thenReturn(lowerMimeType);
        when(builder.lower(filenamePath)).thenReturn(lowerFilename);
        when(builder.lower(pathFromRootPath)).thenReturn(lowerPathFromRoot);
        when(builder.like(lowerMimeType, "image/%")).thenReturn(contentTypePredicate);
        when(builder.like(lowerFilename, "%readme%")).thenReturn(filenamePredicate);
        when(builder.like(lowerPathFromRoot, "%readme%")).thenReturn(pathPredicate);
        when(builder.or(filenamePredicate, pathPredicate)).thenReturn(searchPredicate);
        when(builder.and(providerPredicate, visibilityPredicate, contentTypePredicate, searchPredicate)).thenReturn(combinedPredicate);

        var result = specification.toPredicate(root, query, builder);

        assertSame(combinedPredicate, result);
        verify(builder).like(lowerMimeType, "image/%");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void searchIndexItems_WithPublicFilter_AddsFolderOrPublicAssetPredicate() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.searchIndexItems(3, "Readme", null, true, PageRequest.of(0, 10));

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Specification<VStorageIndexItemWithAssetEntity> specification = specificationCaptor.getValue();

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        Path storageProviderIdPath = mock(Path.class);
        Path directoryPath = mock(Path.class);
        Path assetIsPrivatePath = mock(Path.class);
        Path filenamePath = mock(Path.class);
        Path pathFromRootPath = mock(Path.class);
        Expression lowerFilename = mock(Expression.class);
        Expression lowerPathFromRoot = mock(Expression.class);
        Predicate providerPredicate = mock(Predicate.class);
        Predicate folderPredicate = mock(Predicate.class);
        Predicate publicAssetPredicate = mock(Predicate.class);
        Predicate visibilityPredicate = mock(Predicate.class);
        Predicate contentTypePredicate = mock(Predicate.class);
        Predicate filenamePredicate = mock(Predicate.class);
        Predicate pathPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("storageProviderId")).thenReturn(storageProviderIdPath);
        when(root.get("directory")).thenReturn(directoryPath);
        when(root.get("assetIsPrivate")).thenReturn(assetIsPrivatePath);
        when(root.get("filename")).thenReturn(filenamePath);
        when(root.get("pathFromRoot")).thenReturn(pathFromRootPath);

        when(storageProviderIdPath.as(Integer.class)).thenReturn(storageProviderIdPath);
        when(directoryPath.as(Boolean.class)).thenReturn(directoryPath);
        when(assetIsPrivatePath.as(Boolean.class)).thenReturn(assetIsPrivatePath);
        when(filenamePath.as(String.class)).thenReturn(filenamePath);
        when(pathFromRootPath.as(String.class)).thenReturn(pathFromRootPath);

        when(builder.equal(storageProviderIdPath, 3)).thenReturn(providerPredicate);
        when(builder.isTrue(directoryPath)).thenReturn(folderPredicate);
        when(builder.isFalse(assetIsPrivatePath)).thenReturn(publicAssetPredicate);
        when(builder.or(folderPredicate, publicAssetPredicate)).thenReturn(visibilityPredicate);
        when(builder.conjunction()).thenReturn(contentTypePredicate);
        when(builder.lower(filenamePath)).thenReturn(lowerFilename);
        when(builder.lower(pathFromRootPath)).thenReturn(lowerPathFromRoot);
        when(builder.like(lowerFilename, "%readme%")).thenReturn(filenamePredicate);
        when(builder.like(lowerPathFromRoot, "%readme%")).thenReturn(pathPredicate);
        when(builder.or(filenamePredicate, pathPredicate)).thenReturn(searchPredicate);
        when(builder.and(providerPredicate, visibilityPredicate, contentTypePredicate, searchPredicate)).thenReturn(combinedPredicate);

        var result = specification.toPredicate(root, query, builder);

        assertSame(combinedPredicate, result);
        verify(builder).isTrue(directoryPath);
        verify(builder).isFalse(assetIsPrivatePath);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void searchIndexItems_WithPrivateFilter_AddsPrivateFilePredicate() {
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.searchIndexItems(3, "Readme", null, false, PageRequest.of(0, 10));

        var specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(specificationCaptor.capture(), any(Pageable.class));

        Specification<VStorageIndexItemWithAssetEntity> specification = specificationCaptor.getValue();

        Root root = mock(Root.class);
        CriteriaQuery query = mock(CriteriaQuery.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);

        Path storageProviderIdPath = mock(Path.class);
        Path directoryPath = mock(Path.class);
        Path assetIsPrivatePath = mock(Path.class);
        Path filenamePath = mock(Path.class);
        Path pathFromRootPath = mock(Path.class);
        Expression lowerFilename = mock(Expression.class);
        Expression lowerPathFromRoot = mock(Expression.class);
        Predicate providerPredicate = mock(Predicate.class);
        Predicate filePredicate = mock(Predicate.class);
        Predicate privateAssetPredicate = mock(Predicate.class);
        Predicate unknownVisibilityPredicate = mock(Predicate.class);
        Predicate privateVisibilityPredicate = mock(Predicate.class);
        Predicate visibilityPredicate = mock(Predicate.class);
        Predicate contentTypePredicate = mock(Predicate.class);
        Predicate filenamePredicate = mock(Predicate.class);
        Predicate pathPredicate = mock(Predicate.class);
        Predicate searchPredicate = mock(Predicate.class);
        Predicate combinedPredicate = mock(Predicate.class);

        when(root.get("storageProviderId")).thenReturn(storageProviderIdPath);
        when(root.get("directory")).thenReturn(directoryPath);
        when(root.get("assetIsPrivate")).thenReturn(assetIsPrivatePath);
        when(root.get("filename")).thenReturn(filenamePath);
        when(root.get("pathFromRoot")).thenReturn(pathFromRootPath);

        when(storageProviderIdPath.as(Integer.class)).thenReturn(storageProviderIdPath);
        when(directoryPath.as(Boolean.class)).thenReturn(directoryPath);
        when(assetIsPrivatePath.as(Boolean.class)).thenReturn(assetIsPrivatePath);
        when(filenamePath.as(String.class)).thenReturn(filenamePath);
        when(pathFromRootPath.as(String.class)).thenReturn(pathFromRootPath);

        when(builder.equal(storageProviderIdPath, 3)).thenReturn(providerPredicate);
        when(builder.isFalse(directoryPath)).thenReturn(filePredicate);
        when(builder.isTrue(assetIsPrivatePath)).thenReturn(privateAssetPredicate);
        when(builder.isNull(assetIsPrivatePath)).thenReturn(unknownVisibilityPredicate);
        when(builder.or(privateAssetPredicate, unknownVisibilityPredicate)).thenReturn(privateVisibilityPredicate);
        when(builder.and(filePredicate, privateVisibilityPredicate)).thenReturn(visibilityPredicate);
        when(builder.conjunction()).thenReturn(contentTypePredicate);
        when(builder.lower(filenamePath)).thenReturn(lowerFilename);
        when(builder.lower(pathFromRootPath)).thenReturn(lowerPathFromRoot);
        when(builder.like(lowerFilename, "%readme%")).thenReturn(filenamePredicate);
        when(builder.like(lowerPathFromRoot, "%readme%")).thenReturn(pathPredicate);
        when(builder.or(filenamePredicate, pathPredicate)).thenReturn(searchPredicate);
        when(builder.and(providerPredicate, visibilityPredicate, contentTypePredicate, searchPredicate)).thenReturn(combinedPredicate);

        var result = specification.toPredicate(root, query, builder);

        assertSame(combinedPredicate, result);
        verify(builder).isFalse(directoryPath);
        verify(builder).isTrue(assetIsPrivatePath);
        verify(builder).isNull(assetIsPrivatePath);
    }
}
