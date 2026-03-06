package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.GoverBackend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.GoverBackend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class VStorageIndexItemWithAssetService implements ReadEntityService<VStorageIndexItemWithAssetEntity, VStorageIndexItemWithAssetEntityId> {
    private final VStorageIndexItemWithAssetRepository repository;

    @Autowired
    public VStorageIndexItemWithAssetService(VStorageIndexItemWithAssetRepository repository) {
        this.repository = repository;
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
}
