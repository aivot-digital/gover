package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

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
                           GoverConfig goverConfig, SystemRoleRepository systemRoleRepository) {
        this.auditService = auditService.createScopedAuditService(UserSyncService.class);

        this.userRepository = userRepository;
        this.keycloakApiService = keycloakApiService;
        this.goverConfig = goverConfig;
        this.systemRoleRepository = systemRoleRepository;
    }

    @Scheduled(fixedDelay = delayInMS)
    public void syncUsers() {
        var superRoleId = systemRoleRepository
                .findByMaxPermissions()
                .map(SystemRoleEntity::getId)
                .orElse(0);

        var hasSuperAdmin = userRepository
                .existsBySystemRoleId(superRoleId);

        var alreadyImportedUsers = userRepository
                .findAll();

        Collection<KeycloakUser> keycloakUsersToImport;
        try {
            keycloakUsersToImport = keycloakApiService
                    .listUsers();
        } catch (ResponseException e) {
            throw new RuntimeException(e.getMessage() + " - " + e.getDetails(), e);
        }

        var updatedUserIds = new HashSet<String>();

        // Iterate over all keycloak users and import or update them in the local database
        for (var keycloakUser : keycloakUsersToImport) {
            var userEntity = userRepository
                    .findById(keycloakUser.getId())
                    .orElse(
                            new UserEntity()
                                    .setId(keycloakUser.getId())
                    )
                    .setDeletedInIdp(false)
                    .setEnabled(keycloakUser.getEnabled())
                    .setVerified(keycloakUser.getEmailVerified())
                    .setEmail(keycloakUser.getEmail())
                    .setFirstName(keycloakUser.getFirstName())
                    .setLastName(keycloakUser.getLastName());

            if (!hasSuperAdmin &&
                    goverConfig.getBootstrapAdminMail() != null &&
                    goverConfig.getBootstrapAdminMail().contains(userEntity.getEmail())) {
                auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyMessage("User with id " + userEntity.getId() + " was promoted to a super admin because no super admin exists and their e-mail address is in the list of bootstrapAdminMail", Map.of(
                                "userId", userEntity.getId()
                        )));

                userEntity.setSystemRoleId(superRoleId);
            }

            userRepository
                    .save(userEntity);

            updatedUserIds.add(userEntity.getId());

            auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyMessage("User with id " + userEntity.getId() + " was imported or updated from the IDP", Map.of(
                            "userId", userEntity.getId()
                    )));
        }

        for (var localUser : alreadyImportedUsers) {
            if (updatedUserIds.contains(localUser.getId())) {
                continue;
            }

            // Check if the user ID is a placeholder ID (e.g., "0000-000-0000", "0000-000-0001", etc.)
            if (localUser.getId().matches("^[0-]+[0-9]{3}$")) {
                continue;
            }

            localUser.clearPersonalData();
            localUser.setDeletedInIdp(true);
            localUser.setEnabled(false);

            userRepository
                    .save(localUser);

            auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyMessage("User with id " + localUser.getId() + " was deleted in IDP", Map.of(
                            "userId", localUser.getId()
                    )));
        }
    }
}
