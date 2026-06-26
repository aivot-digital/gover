package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.utils.PaginationUtils;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VStorageIndexItemWithAssetService implements ReadEntityService<VStorageIndexItemWithAssetEntity, VStorageIndexItemWithAssetEntityId> {
    private final VStorageIndexItemWithAssetRepository repository;

    @Autowired
    public VStorageIndexItemWithAssetService(VStorageIndexItemWithAssetRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    public Page<VStorageIndexItemWithAssetEntity> searchIndexItems(@Nonnull Integer providerId,
                                                                   @Nullable String search,
                                                                   @Nullable List<String> contentTypes,
                                                                   @Nullable Boolean isPublic,
                                                                   @Nonnull Pageable pageable) {
        var effectivePageable = createSearchPageable(pageable);

        if (StringUtils.isNullOrEmpty(search)) {
            return Page.empty(effectivePageable);
        }

        var searchPattern = "%" + search.trim().toLowerCase() + "%";
        var normalizedContentTypes = normalizeContentTypes(contentTypes);

        Specification<VStorageIndexItemWithAssetEntity> specification = (root, query, builder) -> builder.and(
                builder.equal(root.get("storageProviderId").as(Integer.class), providerId),
                createVisibilityPredicate(root, builder, isPublic),
                createContentTypePredicate(root, builder, normalizedContentTypes),
                builder.or(
                        builder.like(builder.lower(root.get("filename").as(String.class)), searchPattern),
                        builder.like(builder.lower(root.get("pathFromRoot").as(String.class)), searchPattern)
                )
        );

        return repository.findAll(specification, effectivePageable);
    }

    @Nonnull
    private static jakarta.persistence.criteria.Predicate createVisibilityPredicate(
            @Nonnull jakarta.persistence.criteria.Root<VStorageIndexItemWithAssetEntity> root,
            @Nonnull jakarta.persistence.criteria.CriteriaBuilder builder,
            @Nullable Boolean isPublic
    ) {
        if (isPublic == null) {
            return builder.conjunction();
        }

        var directoryPath = root.get("directory").as(Boolean.class);
        var assetIsPrivatePath = root.get("assetIsPrivate").as(Boolean.class);

        if (isPublic) {
            return builder.or(
                    builder.isTrue(directoryPath),
                    builder.isFalse(assetIsPrivatePath)
            );
        }

        return builder.and(
                builder.isFalse(directoryPath),
                builder.or(
                        builder.isTrue(assetIsPrivatePath),
                        builder.isNull(assetIsPrivatePath)
                )
        );
    }

    @Nonnull
    private static jakarta.persistence.criteria.Predicate createContentTypePredicate(
            @Nonnull jakarta.persistence.criteria.Root<VStorageIndexItemWithAssetEntity> root,
            @Nonnull jakarta.persistence.criteria.CriteriaBuilder builder,
            @Nonnull List<String> contentTypes
    ) {
        if (contentTypes.isEmpty()) {
            return builder.conjunction();
        }

        var mimeTypePath = builder.lower(root.get("mimeType").as(String.class));
        var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>(contentTypes.size());

        for (var contentType : contentTypes) {
            predicates.add(createContentTypeMatchPredicate(builder, mimeTypePath, contentType));
        }

        if (predicates.size() == 1) {
            return predicates.get(0);
        }

        return builder.or(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
    }

    @Nonnull
    private static jakarta.persistence.criteria.Predicate createContentTypeMatchPredicate(
            @Nonnull jakarta.persistence.criteria.CriteriaBuilder builder,
            @Nonnull jakarta.persistence.criteria.Expression<String> mimeTypePath,
            @Nonnull String contentType
    ) {
        if (contentType.endsWith("/")) {
            return builder.like(mimeTypePath, contentType + "%");
        }

        if (!contentType.contains("/")) {
            return builder.like(mimeTypePath, contentType + "/%");
        }

        return builder.equal(mimeTypePath, contentType);
    }

    @Nonnull
    private static List<String> normalizeContentTypes(@Nullable List<String> contentTypes) {
        if (contentTypes == null) {
            return List.of();
        }

        return contentTypes.stream()
                .filter(contentType -> !StringUtils.isNullOrEmpty(contentType))
                .map(contentType -> contentType.trim().toLowerCase())
                .toList();
    }

    @Nullable
    @Override
    public Page<VStorageIndexItemWithAssetEntity> performList(@Nonnull Pageable pageable,
                                                              @Nullable Specification<VStorageIndexItemWithAssetEntity> specification,
                                                              @Nullable Filter<VStorageIndexItemWithAssetEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<VStorageIndexItemWithAssetEntity> retrieve(@Nonnull VStorageIndexItemWithAssetEntityId id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<VStorageIndexItemWithAssetEntity> retrieve(@Nonnull Specification<VStorageIndexItemWithAssetEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    public Optional<VStorageIndexItemWithAssetEntity> retrieveByAssetKey(@Nonnull UUID assetKey) throws ResponseException {
        return repository.findByAssetKey(assetKey);
    }

    @Override
    public boolean exists(@Nonnull VStorageIndexItemWithAssetEntityId id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<VStorageIndexItemWithAssetEntity> specification) {
        return repository.exists(specification);
    }

    @Nonnull
    private static Pageable createSearchPageable(@Nonnull Pageable pageable) {
        var filteredPageable = PaginationUtils.filterSorting(
                pageable,
                "directory",
                "filename",
                "pathFromRoot",
                "mimeType",
                "sizeInBytes",
                "missing",
                "created",
                "updated"
        );

        if (filteredPageable.getSort().isSorted()) {
            return filteredPageable;
        }

        return PageRequest.of(
                filteredPageable.getPageNumber(),
                filteredPageable.getPageSize(),
                Sort.by(
                        Sort.Order.desc("directory"),
                        Sort.Order.asc("filename"),
                        Sort.Order.asc("pathFromRoot")
                )
        );
    }
}
