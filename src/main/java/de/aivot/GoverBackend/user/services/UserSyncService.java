package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.repositories.SystemRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@EnableScheduling
public class UserSyncService {
    private final static int delayInMS = 1000 * 60 * 5; // 5 minutes

    private final ScopedAuditService auditService;

    private final UserRepository userRepository;
    private final KeyCloakApiService keycloakApiService;

    private final GoverConfig goverConfig;
    private final SystemRoleRepository systemRoleRepository;

    @Autowired
    public UserSyncService(AuditService auditService,
                           UserRepository userRepository,
                           KeyCloakApiService keycloakApiService,
                           GoverConfig goverConfig,
                           SystemRoleRepository systemRoleRepository) {
        this.auditService = auditService.createScopedAuditService(UserSyncService.class);

        this.userRepository = userRepository;
        this.keycloakApiService = keycloakApiService;
        this.goverConfig = goverConfig;
        this.systemRoleRepository = systemRoleRepository;
    }

    @Scheduled(fixedDelay = delayInMS)
    public void syncUsers() {
        var startedAt = LocalDateTime.now();
        var syncedUsers = new ArrayList<Map<String, Object>>();

        var totalUsersFromIdp = 0;
        var importedOrUpdatedCount = 0;
        var deletedInIdpCount = 0;
        var promotedToSuperAdminCount = 0;

        var success = true;
        String failureMessage = null;
        Exception syncFailure = null;

        try {
            var superRoleId = systemRoleRepository
                    .findByMaxPermissions()
                    .map(SystemRoleEntity::getId)
                    .orElse(0);

            var hasSuperAdmin = userRepository
                    .existsBySystemRoleId(superRoleId);

            var alreadyImportedUsers = userRepository
                    .findAll();

            Collection<KeycloakUser> keycloakUsersToImport = keycloakApiService
                    .listUsers();

            totalUsersFromIdp = keycloakUsersToImport.size();

            var seenUserIds = new HashSet<String>();

            // Iterate over all keycloak users and import/update only if data has changed.
            for (var keycloakUser : keycloakUsersToImport) {
                var existingUser = userRepository
                        .findById(keycloakUser.getId())
                        .orElse(null);

                var isCreate = existingUser == null;
                var userEntity = existingUser != null
                        ? existingUser
                        : new UserEntity().setId(keycloakUser.getId());

                var newDeletedInIdp = false;
                var newEnabled = keycloakUser.getEnabled();
                var newVerified = keycloakUser.getEmailVerified();
                var newEmail = keycloakUser.getEmail();
                var newFirstName = keycloakUser.getFirstName();
                var newLastName = keycloakUser.getLastName();

                var dataChanged =
                        !Objects.equals(userEntity.getDeletedInIdp(), newDeletedInIdp) ||
                        !Objects.equals(userEntity.getEnabled(), newEnabled) ||
                        !Objects.equals(userEntity.getVerified(), newVerified) ||
                        !Objects.equals(userEntity.getEmail(), newEmail) ||
                        !Objects.equals(userEntity.getFirstName(), newFirstName) ||
                        !Objects.equals(userEntity.getLastName(), newLastName);

                userEntity
                        .setDeletedInIdp(newDeletedInIdp)
                        .setEnabled(newEnabled)
                        .setVerified(newVerified)
                        .setEmail(newEmail)
                        .setFirstName(newFirstName)
                        .setLastName(newLastName);

                var promotedToSuperAdmin = false;
                if (!hasSuperAdmin &&
                        goverConfig.getBootstrapAdminMail() != null &&
                        goverConfig.getBootstrapAdminMail().contains(userEntity.getEmail())) {
                    if (!Objects.equals(userEntity.getSystemRoleId(), superRoleId)) {
                        userEntity.setSystemRoleId(superRoleId);
                        promotedToSuperAdmin = true;
                        promotedToSuperAdminCount++;
                    }
                    hasSuperAdmin = true;
                }

                var hasChanged = isCreate || dataChanged || promotedToSuperAdmin;
                if (hasChanged) {
                    userRepository
                            .save(userEntity);

                    importedOrUpdatedCount++;

                    syncedUsers.add(Map.of(
                            "userId", userEntity.getId(),
                            "email", userEntity.getEmail() != null ? userEntity.getEmail() : "",
                            "enabled", userEntity.getEnabled(),
                            "verified", userEntity.getVerified(),
                            "deletedInIdp", userEntity.getDeletedInIdp(),
                            "action", isCreate ? "imported" : "updated",
                            "promotedToSuperAdmin", promotedToSuperAdmin
                    ));
                }

                seenUserIds.add(userEntity.getId());
            }

            for (var localUser : alreadyImportedUsers) {
                if (seenUserIds.contains(localUser.getId())) {
                    continue;
                }

                // Check if the user ID is a placeholder ID (e.g., "0000-000-0000", "0000-000-0001", etc.)
                if (localUser.getId().matches("^[0-]+[0-9]{3}$")) {
                    continue;
                }

                var formerEmail = localUser.getEmail();

                localUser.clearPersonalData();
                localUser.setDeletedInIdp(true);
                localUser.setEnabled(false);

                userRepository
                        .save(localUser);

                deletedInIdpCount++;

                syncedUsers.add(Map.of(
                        "userId", localUser.getId(),
                        "email", formerEmail != null ? formerEmail : "",
                        "enabled", false,
                        "verified", false,
                        "deletedInIdp", true,
                        "action", "deleted_in_idp",
                        "promotedToSuperAdmin", false
                ));
            }
        } catch (Exception e) {
            success = false;
            failureMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            syncFailure = e;
        } finally {
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("startedAt", startedAt);
            metadata.put("finishedAt", LocalDateTime.now());
            metadata.put("success", success);
            metadata.put("totalUsersFromIdp", totalUsersFromIdp);
            metadata.put("importedOrUpdatedCount", importedOrUpdatedCount);
            metadata.put("deletedInIdpCount", deletedInIdpCount);
            metadata.put("promotedToSuperAdminCount", promotedToSuperAdminCount);
            metadata.put("failureMessage", failureMessage != null ? failureMessage : "");
            metadata.put("syncedUsers", syncedUsers);

            auditService.addAuditEntry(AuditLogPayload
                    .create()
                    .withSystem()
                    .setTriggerType("UserSync")
                    .setMessage(success ? "User sync completed" : "User sync failed")
                    .setMetadata(metadata));
        }

        if (syncFailure != null) {
            throw new RuntimeException(syncFailure.getMessage(), syncFailure);
        }
    }
}
