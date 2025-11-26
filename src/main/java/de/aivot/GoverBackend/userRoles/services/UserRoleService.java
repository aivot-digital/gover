package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.UserRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserRoleService implements EntityService<UserRoleEntity, Integer> {
    private final UserRoleRepository repository;

    @Autowired
    public UserRoleService(UserRoleRepository repository) {
        this.repository = repository;
    }

    @Nonnull
    @Override
    public UserRoleEntity create(@Nonnull UserRoleEntity entity) throws ResponseException {
        // Directly save the entity
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull UserRoleEntity entity) throws ResponseException {
        // Directly delete the entity
        repository.delete(entity);
    }

    @Nullable
    @Override
    public Page<UserRoleEntity> performList(@Nonnull Pageable pageable, @Nullable Specification<UserRoleEntity> specification, @Nullable Filter<UserRoleEntity> filter) throws ResponseException {
        // List with specification and pagination
        return repository.findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public UserRoleEntity performUpdate(@Nonnull Integer id, @Nonnull UserRoleEntity entity, @Nonnull UserRoleEntity existingEntity) throws ResponseException {
        // Update fields
        existingEntity.setName(entity.getName());
        existingEntity.setDescription(entity.getDescription());

        // Update permissions the parent entity
        existingEntity.setDepartmentPermissionEdit(entity.getDepartmentPermissionEdit());
        existingEntity.setTeamPermissionEdit(entity.getTeamPermissionEdit());

        // Update permissions for forms
        existingEntity.setFormPermissionCreate(entity.getFormPermissionCreate());
        existingEntity.setFormPermissionRead(entity.getFormPermissionRead());
        existingEntity.setFormPermissionEdit(entity.getFormPermissionEdit());
        existingEntity.setFormPermissionDelete(entity.getFormPermissionDelete());
        existingEntity.setFormPermissionAnnotate(entity.getFormPermissionAnnotate());
        existingEntity.setFormPermissionPublish(entity.getFormPermissionPublish());

        // Update permissions for processes
        existingEntity.setProcessPermissionCreate(entity.getProcessPermissionCreate());
        existingEntity.setProcessPermissionRead(entity.getProcessPermissionRead());
        existingEntity.setProcessPermissionEdit(entity.getProcessPermissionEdit());
        existingEntity.setProcessPermissionDelete(entity.getProcessPermissionDelete());
        existingEntity.setProcessPermissionAnnotate(entity.getProcessPermissionAnnotate());
        existingEntity.setProcessPermissionPublish(entity.getProcessPermissionPublish());

        // Update permissions for process instances
        existingEntity.setProcessInstancePermissionCreate(entity.getProcessInstancePermissionCreate());
        existingEntity.setProcessInstancePermissionRead(entity.getProcessInstancePermissionRead());
        existingEntity.setProcessInstancePermissionEdit(entity.getProcessInstancePermissionEdit());
        existingEntity.setProcessInstancePermissionDelete(entity.getProcessInstancePermissionDelete());
        existingEntity.setProcessInstancePermissionAnnotate(entity.getProcessInstancePermissionAnnotate());

        // Save updated entity
        return repository.save(existingEntity);
    }

    @Nonnull
    @Override
    public Optional<UserRoleEntity> retrieve(@Nonnull Integer id) throws ResponseException {
        return repository.findById(id);
    }

    @Nonnull
    @Override
    public Optional<UserRoleEntity> retrieve(@Nonnull Specification<UserRoleEntity> specification) throws ResponseException {
        return repository.findOne(specification);
    }

    @Override
    public boolean exists(@Nonnull Integer id) {
        return repository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<UserRoleEntity> specification) {
        return repository.exists(specification);
    }
}
