package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.config.services.SystemConfigService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves the system role that should be assigned when a user enters Gover through
 * an automatic path such as IdP synchronization or on-demand import on first login.
 *
 * <p>This keeps the import policy in one place so the scheduled sync and the
 * first-login import use the same rules:
 * <ul>
 *   <li>promote the configured bootstrap admin mail to super-admin if none exists yet</li>
 *   <li>otherwise keep an already assigned system role</li>
 *   <li>otherwise assign the configured default imported-user role</li>
 * </ul>
 *
 * <p>Manual staff user creation is intentionally not handled here because that path
 * already requires an explicit system role selection.
 */
@Service
public class ImportedUserSystemRoleService {
    private final GoverConfig goverConfig;
    private final SystemConfigService systemConfigService;
    private final SystemRoleRepository systemRoleRepository;
    private final UserRepository userRepository;

    @Autowired
    public ImportedUserSystemRoleService(
            GoverConfig goverConfig,
            SystemConfigService systemConfigService,
            SystemRoleRepository systemRoleRepository,
            UserRepository userRepository
    ) {
        this.goverConfig = goverConfig;
        this.systemConfigService = systemConfigService;
        this.systemRoleRepository = systemRoleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Result of resolving the target system role for an imported user.
     *
     * @param systemRoleId The role that should be stored on the user.
     * @param promotedToSuperAdmin Whether the resolution newly promoted the user to super-admin.
     */
    public record ImportedUserSystemRoleResolution(
            @Nullable Integer systemRoleId,
            boolean promotedToSuperAdmin
    ) {
    }

    /**
     * Resolves the imported user's target role using the currently configured default role,
     * the current super-admin role, and the current super-admin presence in the database.
     */
    @Nonnull
    public ImportedUserSystemRoleResolution resolveSystemRoleId(
            @Nullable String email,
            @Nullable Integer currentSystemRoleId
    ) throws ResponseException {
        var defaultSystemRoleId = getDefaultSystemRoleId();
        var superAdminRoleId = getSuperAdminRoleId();
        var hasSuperAdmin = hasSuperAdminUser(superAdminRoleId);

        return resolveSystemRoleId(email, currentSystemRoleId, defaultSystemRoleId, superAdminRoleId, hasSuperAdmin);
    }

    /**
     * Resolves the imported user's target role from already prepared inputs.
     *
     * <p>This overload is primarily useful for sync flows and tests that already know
     * the default role id, the super-admin role id, and whether a super-admin exists.
     */
    @Nonnull
    public ImportedUserSystemRoleResolution resolveSystemRoleId(
            @Nullable String email,
            @Nullable Integer currentSystemRoleId,
            @Nonnull Integer defaultSystemRoleId,
            @Nullable Integer superAdminRoleId,
            boolean hasSuperAdmin
    ) {
        if (!hasSuperAdmin && superAdminRoleId != null && isBootstrapAdmin(email)) {
            return new ImportedUserSystemRoleResolution(superAdminRoleId, !superAdminRoleId.equals(currentSystemRoleId));
        }

        if (currentSystemRoleId != null) {
            return new ImportedUserSystemRoleResolution(currentSystemRoleId, false);
        }

        return new ImportedUserSystemRoleResolution(defaultSystemRoleId, false);
    }

    /**
     * Reads the configured default role for imported users and ensures that it still
     * references an existing system role.
     */
    @Nonnull
    public Integer getDefaultSystemRoleId() throws ResponseException {
        var configEntity = systemConfigService
                .retrieve(DefaultUserSystemRoleSystemConfigDefinition.KEY);

        var systemRoleId = configEntity
                .getValueAsInteger()
                .orElseThrow(() -> ResponseException.internalServerError("Die konfigurierte Standard-Systemrolle für automatische Benutzerimporte ist ungültig."));

        if (!systemRoleRepository.existsById(systemRoleId)) {
            throw ResponseException.internalServerError("Die konfigurierte Standard-Systemrolle für automatische Benutzerimporte existiert nicht.");
        }

        return systemRoleId;
    }

    /**
     * Returns the role currently treated as super-admin, based on the role with the
     * largest permission set.
     */
    @Nullable
    public Integer getSuperAdminRoleId() {
        return systemRoleRepository
                .findByMaxPermissions()
                .map(SystemRoleEntity::getId)
                .orElse(null);
    }

    /**
     * Checks whether any user currently holds the resolved super-admin role.
     */
    public boolean hasSuperAdminUser(@Nullable Integer superAdminRoleId) {
        return superAdminRoleId != null && userRepository.existsBySystemRoleId(superAdminRoleId);
    }

    private boolean isBootstrapAdmin(@Nullable String email) {
        return email != null &&
               goverConfig.getBootstrapAdminMail() != null &&
               goverConfig.getBootstrapAdminMail().contains(email);
    }
}
