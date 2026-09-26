package de.aivot.prosuna.backend.userRoles.services;

import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.lib.services.EntityService;
import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.userRoles.configs.MostPrivilegedSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.userRoles.entities.SystemRoleEntity;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class SystemRoleService implements EntityService<SystemRoleEntity, Integer> {
    private final SystemRoleRepository repository;
    private final SystemConfigService systemConfigService;
    private final UserRepository userRepository;
    private final List<PermissionProvider> permissionProviders;

    @Autowired
    public SystemRoleService(
            SystemRoleRepository repository,
            SystemConfigService systemConfigService,
            UserRepository userRepository,
            List<PermissionProvider> permissionProviders
    ) {
        this.repository = repository;
        this.systemConfigService = systemConfigService;
        this.userRepository = userRepository;
        this.permissionProviders = permissionProviders;
    }

    public record DeleteSystemRoleResult(
            @Nullable SystemRoleEntity replacementRole,
            int migratedUsersCount,
            @Nonnull List<MigratedUserAuditInfo> migratedUsers,
            boolean defaultSystemRoleForAutomaticImportsUpdated,
            @Nullable Integer newDefaultSystemRoleId,
            boolean mostPrivilegedSystemRoleUpdated,
            @Nullable Integer newMostPrivilegedSystemRoleId
    ) {
    }

    public record MigratedUserAuditInfo(
            @Nonnull String id,
            @Nonnull String fullName,
            @Nullable String email
    ) {
    }

    @Nonnull
    @Override
    public SystemRoleEntity create(@Nonnull SystemRoleEntity entity) throws ResponseException {
        validateSystemRolePermissionsForCreate(entity.getPermissions());

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

        var mostPrivilegedSystemRoleConfig = systemConfigService
                .retrieve(MostPrivilegedSystemRoleSystemConfigDefinition.KEY);
        var mostPrivilegedSystemRoleId = mostPrivilegedSystemRoleConfig
                .getValueAsInteger()
                .orElseThrow(() -> ResponseException.internalServerError(
                        "Die konfigurierte Systemrolle mit der höchsten Berechtigungsstufe ist ungültig."
                ));

        var affectsDefaultSystemRoleForAutomaticImports = roleToDeleteId.equals(defaultSystemRoleId);
        var affectsMostPrivilegedSystemRole = roleToDeleteId.equals(mostPrivilegedSystemRoleId);
        var hasAssignedUsers = Boolean.TRUE.equals(userRepository.existsBySystemRoleId(roleToDeleteId));
        var replacementRoleRequired = affectsDefaultSystemRoleForAutomaticImports ||
                                      affectsMostPrivilegedSystemRole ||
                                      hasAssignedUsers;

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

        var migratedUsers = hasAssignedUsers
                ? userRepository.findAllBySystemRoleIdOrderByFullNameAsc(roleToDeleteId)
                .stream()
                .map(user -> new MigratedUserAuditInfo(
                        user.getId(),
                        user.getFullName(),
                        user.getEmail()
                ))
                .toList()
                : List.<MigratedUserAuditInfo>of();

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

        var mostPrivilegedSystemRoleUpdated = false;
        Integer newMostPrivilegedSystemRoleId = null;
        if (affectsMostPrivilegedSystemRole && replacementRole != null) {
            mostPrivilegedSystemRoleConfig.setValue(String.valueOf(replacementRole.getId()));
            systemConfigService.save(
                    MostPrivilegedSystemRoleSystemConfigDefinition.KEY,
                    mostPrivilegedSystemRoleConfig
            );
            mostPrivilegedSystemRoleUpdated = true;
            newMostPrivilegedSystemRoleId = replacementRole.getId();
        }

        repository.delete(roleToDelete);

        return new DeleteSystemRoleResult(
                replacementRole,
                migratedUsersCount,
                migratedUsers,
                defaultSystemRoleForAutomaticImportsUpdated,
                newDefaultSystemRoleId,
                mostPrivilegedSystemRoleUpdated,
                newMostPrivilegedSystemRoleId
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
        validateSystemRolePermissionsForUpdate(entity.getPermissions(), existingEntity.getPermissions());

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

    private void validateSystemRolePermissionsForCreate(@Nonnull List<String> permissions) throws ResponseException {
        var unknownPermissions = new HashSet<>(permissions);
        unknownPermissions.removeAll(getKnownPermissions());

        if (!unknownPermissions.isEmpty()) {
            throwUnknownSystemRolePermissions(unknownPermissions);
        }
    }

    private void validateSystemRolePermissionsForUpdate(@Nonnull List<String> permissions,
                                                        @Nonnull List<String> existingPermissions) throws ResponseException {
        var unknownNewPermissions = new HashSet<>(permissions);
        unknownNewPermissions.removeAll(getKnownPermissions());

        // Existing unknown grants are tolerated on update so users can still save the role
        // while incrementally removing legacy permissions that no provider exposes anymore.
        unknownNewPermissions.removeAll(existingPermissions);

        if (!unknownNewPermissions.isEmpty()) {
            throwUnknownSystemRolePermissions(unknownNewPermissions);
        }
    }

    private Set<String> getKnownPermissions() {
        var knownPermissions = new HashSet<String>();
        permissionProviders
                .stream()
                .flatMap(provider -> Arrays.stream(provider.getPermissions()))
                .map(permission -> permission.permission())
                .forEach(knownPermissions::add);

        return knownPermissions;
    }

    private void throwUnknownSystemRolePermissions(@Nonnull Set<String> unknownPermissions) throws ResponseException {
        throw ResponseException.badRequest(
                "Die folgenden Berechtigungen sind im System nicht bekannt: %s.",
                String.join(", ", unknownPermissions)
        );
    }
}
