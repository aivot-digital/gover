package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SystemRoleService implements EntityService<SystemRoleEntity, Integer> {
    private final SystemRoleRepository repository;

    @Autowired
    public SystemRoleService(SystemRoleRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public SystemRoleEntity create(@Nonnull SystemRoleEntity entity) throws ResponseException {
        // Directly save the entity
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull SystemRoleEntity entity) throws ResponseException {
        // Directly delete the entity
        repository.delete(entity);
    }

    @Nullable
    @Override
    public Page<SystemRoleEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<SystemRoleEntity> specification, @Nullable Filter<SystemRoleEntity> filter) throws ResponseException {
        // List with specification and pagination
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public SystemRoleEntity performUpdate(@Nonnull Integer id, @Nonnull SystemRoleEntity entity, @Nonnull SystemRoleEntity existingEntity) throws ResponseException {
        // Update fields
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());

        // Update permissions the parent entity
        existingEntity.setPermissions(entity.getPermissions());

        // Save updated entity
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<SystemRoleEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<SystemRoleEntity> retrieve(@Nonnull Specification<SystemRoleEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<SystemRoleEntity> specification) {
        return repository.exists(specification);
    }
}
