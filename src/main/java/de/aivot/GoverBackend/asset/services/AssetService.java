package de.aivot.GoverBackend.asset.services;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.repositories.AssetRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssetService implements EntityService<AssetEntity, String> {
    private final AssetRepository repository;

    @Autowired
    public AssetService(AssetRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public AssetEntity create(@Nonnull AssetEntity entity) throws ResponseException {
        entity.setKey(UUID.randomUUID().toString());
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull AssetEntity asset) throws ResponseException {
        repository.delete(asset);
    }

    @Nonnull
    @Override
    public Page<AssetEntity> performList(
            @Nonnull Pageable pageable,
            @Nonnull Specification<AssetEntity> specification,
            Filter<AssetEntity> filter) {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public AssetEntity performUpdate(
            @Nonnull String id,
            @Nonnull AssetEntity entity,
            @Nonnull AssetEntity existingEntity
    ) throws ResponseException {
        existingEntity.setFilename(entity.getFilename());
        existingEntity.setPrivate(entity.getPrivate());
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(@Nonnull String id) {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<AssetEntity> retrieve(
            @Nonnull Specification<AssetEntity> specification
    ) {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull String id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(
            @Nonnull Specification<AssetEntity> specification
    ) {
        return repository.exists(specification);
    }
}
