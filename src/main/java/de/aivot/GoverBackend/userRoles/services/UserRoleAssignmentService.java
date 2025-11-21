package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.repositories.UserRoleAssignmentRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserRoleAssignmentService implements EntityService<UserRoleAssignmentEntity, Integer> {
    private final UserRoleAssignmentRepository repository;

    @Autowired
    public UserRoleAssignmentService(UserRoleAssignmentRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public UserRoleAssignmentEntity create(@Nonnull UserRoleAssignmentEntity entity) throws ResponseException {
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull UserRoleAssignmentEntity entity) throws ResponseException {
        repository.delete(entity);
    }

    @Nullable
    @Override
    public Page<UserRoleAssignmentEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<UserRoleAssignmentEntity> specification, @Nullable Filter<UserRoleAssignmentEntity> filter) throws ResponseException {
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public UserRoleAssignmentEntity performUpdate(@Nonnull Integer id, @Nonnull UserRoleAssignmentEntity entity, @Nonnull UserRoleAssignmentEntity existingEntity) throws ResponseException {
        // This entity has no updatable fields
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<UserRoleAssignmentEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<UserRoleAssignmentEntity> retrieve(@Nonnull Specification<UserRoleAssignmentEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<UserRoleAssignmentEntity> specification) {
        return repository.exists(specification);
    }
}
