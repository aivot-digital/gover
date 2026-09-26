package de.aivot.prosuna.backend.userRoles.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.userRoles.entities.UserRoleEntity;
import de.aivot.prosuna.backend.userRoles.repositories.UserRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserRoleService implements EntityService<UserRoleEntity, Integer> {
    private final UserRoleRepository repository;
    private final List<PermissionProvider> permissionProviders;

    @Autowired
    public UserRoleService(UserRoleRepository repository,
                           List<PermissionProvider> permissionProviders) {
        this.repository = repository;
        this.permissionProviders = permissionProviders;
    }

    @Nonnull
    @Override
    public UserRoleEntity create(@Nonnull UserRoleEntity entity) throws ResponseException {
        validateDomainRolePermissionsForCreate(entity.getPermissions());

        // Force the generation of a new id
        entity.setId(null);
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
        validateDomainRolePermissionsForUpdate(entity.getPermissions(), existingEntity.getPermissions());

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

    private void validateDomainRolePermissionsForCreate(@Nonnull List<String> permissions) throws ResponseException {
        var unsupportedPermissions = new HashSet<>(permissions);
        unsupportedPermissions.removeAll(getDomainRoleAssignablePermissions());

        if (!unsupportedPermissions.isEmpty()) {
            throwUnsupportedDomainRolePermissions(unsupportedPermissions);
        }
    }

    private void validateDomainRolePermissionsForUpdate(@Nonnull List<String> permissions,
                                                        @Nonnull List<String> existingPermissions) throws ResponseException {
        var unsupportedNewPermissions = new HashSet<>(permissions);
        unsupportedNewPermissions.removeAll(getDomainRoleAssignablePermissions());

        // Existing unsupported grants are tolerated on update so users can still save the role
        // while incrementally removing legacy permissions that are no longer domain-assignable.
        unsupportedNewPermissions.removeAll(existingPermissions);

        if (!unsupportedNewPermissions.isEmpty()) {
            throwUnsupportedDomainRolePermissions(unsupportedNewPermissions);
        }
    }

    private Set<String> getDomainRoleAssignablePermissions() {
        var supportedPermissions = new HashSet<String>();
        permissionProviders
                .stream()
                .filter(PermissionProvider::supportsDomainRoleAssignment)
                .flatMap(provider -> {
                    var excludedPermissions = provider.getExcludedFromDomainRoleAssignment();

                    return Arrays.stream(provider.getPermissions())
                            .filter(permission -> !excludedPermissions.contains(permission.permission()));
                })
                .map(permission -> permission.permission())
                .forEach(supportedPermissions::add);

        return supportedPermissions;
    }

    private void throwUnsupportedDomainRolePermissions(@Nonnull Set<String> unsupportedPermissions) throws ResponseException {
        throw ResponseException.badRequest(
                "Die folgenden Berechtigungen können keiner Domänenrolle zugewiesen werden: %s.",
                String.join(", ", unsupportedPermissions)
        );
    }
}
