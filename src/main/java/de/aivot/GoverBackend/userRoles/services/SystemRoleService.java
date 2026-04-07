package de.aivot.GoverBackend.userRoles.services;

import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SystemRoleService implements EntityService<SystemRoleEntity, Integer> {
    private final SystemRoleRepository repository;
    private final SystemConfigService systemConfigService;
    private final UserRepository userRepository;

    @Autowired
    public SystemRoleService(
            SystemRoleRepository repository,
            SystemConfigService systemConfigService,
            UserRepository userRepository
    ) {
        this.repository = repository;
        this.systemConfigService = systemConfigService;
        this.userRepository = userRepository;
    }

    public record DeleteSystemRoleResult(
            @Nullable SystemRoleEntity replacementRole,
            int migratedUsersCount,
            boolean defaultSystemRoleForAutomaticImportsUpdated,
            @Nullable Integer newDefaultSystemRoleId
    ) {
    }

    @Nonnull
    @Override
    public SystemRoleEntity create(@Nonnull SystemRoleEntity entity) throws ResponseException {
        // Force the generation of a new id
        entity.setId(null);
        // Directly save the entity
        return repository.save(entity);
    }

    @Override
    public void performDelete(@Nonnull SystemRoleEntity entity) throws ResponseException {
        deleteAndMigrateUsers(entity, null);
    }

    @Nonnull
    @Transactional
    public DeleteSystemRoleResult deleteAndMigrateUsers(
            @Nonnull SystemRoleEntity roleToDelete,
            @Nullable Integer replacementSystemRoleId
    ) throws ResponseException {
        var roleToDeleteId = roleToDelete.getId();
        if (roleToDeleteId == null) {
            throw ResponseException.internalServerError("Die zu löschende Systemrolle besitzt keine ID.");
        }

        var defaultSystemRoleConfig = systemConfigService
                .retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY);
        var defaultSystemRoleId = defaultSystemRoleConfig
                .getValueAsInteger()
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Die konfigurierte Standard-Systemrolle für automatische Benutzerimporte ist ungültig."
                ));

        var affectsDefaultSystemRoleForAutomaticImports = roleToDeleteId.equals(defaultSystemRoleId);
        var hasAssignedUsers = Boolean.TRUE.equals(userRepository.existsBySystemRoleId(roleToDeleteId));
        var replacementRoleRequired = affectsDefaultSystemRoleForAutomaticImports || hasAssignedUsers;

        if (replacementSystemRoleId != null && roleToDeleteId.equals(replacementSystemRoleId)) {
            throw ResponseException.badRequest("Bitte wählen Sie eine andere Systemrolle als Ersatz aus.");
        }

        SystemRoleEntity replacementRole = null;
        if (replacementRoleRequired || replacementSystemRoleId != null) {
            if (replacementSystemRoleId == null) {
                throw ResponseException.badRequest(
                        "Bitte wählen Sie eine Ersatz-Systemrolle aus, damit vorhandene Nutzer:innen und Systemeinstellungen migriert werden können."
                );
            }

            replacementRole = repository
                    .findById(replacementSystemRoleId)
                    .orElseThrow(() -> ResponseException.badRequest("Die ausgewählte Ersatz-Systemrolle existiert nicht."));
        }

        var migratedUsersCount = 0;
        if (hasAssignedUsers && replacementRole != null) {
            migratedUsersCount = userRepository.reassignSystemRoleId(roleToDeleteId, replacementRole.getId());
        }

        var defaultSystemRoleForAutomaticImportsUpdated = false;
        Integer newDefaultSystemRoleId = null;
        if (affectsDefaultSystemRoleForAutomaticImports && replacementRole != null) {
            defaultSystemRoleConfig.setValue(String.valueOf(replacementRole.getId()));
            systemConfigService.save(DefaultUserSystemRoleSystemConfigDefinition.KEY, defaultSystemRoleConfig);
            defaultSystemRoleForAutomaticImportsUpdated = true;
            newDefaultSystemRoleId = replacementRole.getId();
        }

        repository.delete(roleToDelete);

        return new DeleteSystemRoleResult(
                replacementRole,
                migratedUsersCount,
                defaultSystemRoleForAutomaticImportsUpdated,
                newDefaultSystemRoleId
        );
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
