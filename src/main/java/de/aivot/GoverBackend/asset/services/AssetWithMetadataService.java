package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.AssetWithMetadataEntity;
import de.aivot.GoverBackend.asset.filters.AssetFilter;
import de.aivot.GoverBackend.asset.repositories.AssetWithMetadataRepository;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetWithMetadataService {
    private final AssetWithMetadataRepository repository;

    @Autowired
    public AssetWithMetadataService(AssetWithMetadataRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    public Page<AssetWithMetadataEntity> list(@Nonnull Pageable pageable,
                                              @Nullable AssetFilter filter) {
        Specification<AssetWithMetadataEntity> spec = SpecificationBuilder
                .create(AssetWithMetadataEntity.class)
                .withContains("filename", filter != null ? filter.getFilename() : null)
                .withEquals("uploaderId", filter != null ? filter.getUploaderId() : null)
                .withContains("contentType", filter != null ? filter.getContentType() : null)
                .withEquals("isPrivate", filter != null ? filter.getPrivate() : null)
                .withEquals("storageProviderId", filter != null ? filter.getStorageProviderId() : null)
                .build();

        return repository.findAll(spec, pageable);
    }

    @Nonnull
    public Optional<AssetWithMetadataEntity> retrieve(@Nonnull UUID key) {
        return repository.findById(key);
    }

    @Nonnull
    public List<Integer> findDistinctStorageProviderIds() {
        return repository.findDistinctStorageProviderIds();
    }

    @Nonnull
    public List<AssetWithMetadataEntity> listAllInFolder(@Nonnull Integer storageProviderId,
                                                         @Nonnull String folderPath) {
        return repository.listAllInFolder(storageProviderId, folderPath);
    }
}
