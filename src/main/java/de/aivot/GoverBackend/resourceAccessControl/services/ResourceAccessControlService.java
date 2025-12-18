package de.aivot.GoverBackend.resourceAccessControl.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import de.aivot.GoverBackend.resourceAccessControl.repositories.ResourceAccessControlRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ResourceAccessControlService implements EntityService<ResourceAccessControlEntity, Integer> {
    private final ResourceAccessControlRepository repository;

    @Autowired
    public ResourceAccessControlService(ResourceAccessControlRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public ResourceAccessControlEntity create(@Nonnull ResourceAccessControlEntity entity) throws ResponseException {
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull ResourceAccessControlEntity entity) throws ResponseException {
        repository.delete(entity);
    }

    @Nullable
    @Override
    public Page<ResourceAccessControlEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<ResourceAccessControlEntity> specification, @Nullable Filter<ResourceAccessControlEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public ResourceAccessControlEntity performUpdate(@Nonnull Integer id, @Nonnull ResourceAccessControlEntity entity, @Nonnull ResourceAccessControlEntity existingEntity) throws ResponseException {
        existingEntity.setPermissions(entity.getPermissions());
        return repository
                .save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<ResourceAccessControlEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<ResourceAccessControlEntity> retrieve(@Nonnull Specification<ResourceAccessControlEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<ResourceAccessControlEntity> specification) {
        return repository.exists(specification);
    }
}

