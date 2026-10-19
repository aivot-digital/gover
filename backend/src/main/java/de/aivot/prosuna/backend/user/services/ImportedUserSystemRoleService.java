package de.aivot.prosuna.backend.user.services;

import de.aivot.prosuna.backend.config.services.SystemConfigService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.models.config.ProsunaConfig;
import de.aivot.prosuna.backend.user.configs.DefaultUserSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.user.repositories.UserRepository;
import de.aivot.prosuna.backend.userRoles.configs.MostPrivilegedSystemRoleSystemConfigDefinition;
import de.aivot.prosuna.backend.userRoles.repositories.SystemRoleRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves the system role that should be assigned when a user enters Prosuna through
 * an automatic path such as IdP synchronization or on-demand import on first login.
 *
 * <p>This keeps the import policy in one place so the scheduled sync and the
 * first-login import use the same rules:
 * <ul>
 *   <li>promote the configured bootstrap admin mail to the most privileged role if no active user holds it</li>
 *   <li>otherwise keep an already assigned system role</li>
 *   <li>otherwise assign the configured default imported-user role</li>
 * </ul>
 *
 * <p>Manual staff user creation is intentionally not handled here because that path
 * already requires an explicit system role selection.
 */
@Service
public class ImportedUserSystemRoleService {
    private final ProsunaConfig prosunaConfig;
    private final SystemConfigService systemConfigService;
    private final SystemRoleRepository systemRoleRepository;
    private final UserRepository userRepository;

    @Autowired
    public ImportedUserSystemRoleService(
            ProsunaConfig prosunaConfig,
            SystemConfigService systemConfigService,
            SystemRoleRepository systemRoleRepository,
            UserRepository userRepository
    ) {
        this.prosunaConfig = prosunaConfig;
        this.systemConfigService = systemConfigService;
        this.systemRoleRepository = systemRoleRepository;
        this.userRepository = userRepository;
    }

    /**
     * Result of resolving the target system role for an imported user.
     *
     * @param systemRoleId The role that should be stored on the user.
     * @param promotedToMostPrivilegedRole Whether the resolution newly promoted the user to the most privileged role.
     */
    public record ImportedUserSystemRoleResolution(
            @Nullable Integer systemRoleId,
            boolean promotedToMostPrivilegedRole
    ) {
    }

    /**
     * Resolves the imported user's target role using the currently configured default role,
     * the most privileged role, and whether an active user currently holds that role.
     */
    @Nonnull
    public ImportedUserSystemRoleResolution resolveSystemRoleId(
            @Nullable String email,
            @Nullable Integer currentSystemRoleId
    ) throws ResponseException {
        var defaultSystemRoleId = getDefaultSystemRoleId();
        var mostPrivilegedSystemRoleId = getMostPrivilegedSystemRoleId();
        var hasActiveUserWithMostPrivilegedRole = hasActiveUserWithMostPrivilegedRole(mostPrivilegedSystemRoleId);

        return resolveSystemRoleId(
                email,
                currentSystemRoleId,
                defaultSystemRoleId,
                mostPrivilegedSystemRoleId,
                hasActiveUserWithMostPrivilegedRole
        );
    }

    /**
     * Resolves the imported user's target role from already prepared inputs.
     *
     * <p>This overload is primarily useful for sync flows and tests that already know
     * the default role id, the most privileged role id, and whether an active user holds that role.
     */
    @Nonnull
    public ImportedUserSystemRoleResolution resolveSystemRoleId(
            @Nullable String email,
            @Nullable Integer currentSystemRoleId,
            @Nonnull Integer defaultSystemRoleId,
            @Nonnull Integer mostPrivilegedSystemRoleId,
            boolean hasActiveUserWithMostPrivilegedRole
    ) {
        if (!hasActiveUserWithMostPrivilegedRole && isBootstrapAdmin(email)) {
            return new ImportedUserSystemRoleResolution(
                    mostPrivilegedSystemRoleId,
                    !mostPrivilegedSystemRoleId.equals(currentSystemRoleId)
            );
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
     * Reads the system role configured as the highest permission level and ensures
     * that it still references an existing role.
     */
    @Nonnull
    public Integer getMostPrivilegedSystemRoleId() throws ResponseException {
        var configEntity = systemConfigService
                .retrieve(MostPrivilegedSystemRoleSystemConfigDefinition.KEY);

        var systemRoleId = configEntity
                .getValueAsInteger()
                .orElseThrow(() -> ResponseException.internalServerError("Die konfigurierte Systemrolle mit der höchsten Berechtigungsstufe ist ungültig."));

        if (!systemRoleRepository.existsById(systemRoleId)) {
            throw ResponseException.internalServerError("Die konfigurierte Systemrolle mit der höchsten Berechtigungsstufe existiert nicht.");
        }

        return systemRoleId;
    }

    /**
     * Checks whether an enabled user that still exists in the identity provider
     * currently holds the most privileged role.
     */
    public boolean hasActiveUserWithMostPrivilegedRole(@Nonnull Integer mostPrivilegedSystemRoleId) {
        return userRepository.existsActiveUserBySystemRoleId(mostPrivilegedSystemRoleId);
    }

    private boolean isBootstrapAdmin(@Nullable String email) {
        return email != null &&
               prosunaConfig.getBootstrapAdminMail() != null &&
               prosunaConfig.getBootstrapAdminMail().contains(email);
    }
}
