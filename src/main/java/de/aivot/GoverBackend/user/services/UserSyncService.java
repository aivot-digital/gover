package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
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

    @Autowired
    public UserSyncService(
            AuditService auditService,
            UserRepository userRepository,
            KeyCloakApiService keycloakApiService
    ) {
        this.auditService = auditService.createScopedAuditService(UserSyncService.class);

        this.userRepository = userRepository;
        this.keycloakApiService = keycloakApiService;
    }

    @Scheduled(fixedDelay = delayInMS)
    public void syncUsers() {
        var localUsers = userRepository
                .findAllByDeletedInIdpIsFalse();

        Collection<KeycloakUser> keycloakUsers;
        try {
            keycloakUsers = keycloakApiService
                    .listUsers();
        } catch (ResponseException e) {
            throw new RuntimeException(e.getMessage() + " - " + e.getDetails(), e);
        }

        var updatedUserIds = new HashSet<String>();

        for (var keycloakUser : keycloakUsers) {
            var userEntity = UserEntity
                    .from(keycloakUser);

            var existingUserOpt = userRepository
                    .findById(userEntity.getId());

            if (existingUserOpt.isPresent()) {
                var existingUser = existingUserOpt
                        .get()
                        .setDeletedInIdp(false)
                        .setEnabled(userEntity.getEnabled())
                        .setVerified(userEntity.getVerified())
                        .setEmail(userEntity.getEmail())
                        .setFirstName(userEntity.getFirstName())
                        .setLastName(userEntity.getLastName());
                userRepository
                        .save(existingUser);
            } else {
                userRepository
                        .save(userEntity);
            }

            updatedUserIds.add(userEntity.getId());
        }

        for (var localUser : localUsers) {
            if (updatedUserIds.contains(localUser.getId())) {
                continue;
            }

            // Check if the user ID is a placeholder ID (e.g., "0000-000-0000", "0000-000-0001", etc.)
            if (localUser.getId().matches("^[0-]+[0-9]$")) {
                continue;
            }

            localUser.clearPersonalData();
            localUser.setDeletedInIdp(true);
            localUser.setEnabled(false);

            userRepository
                    .save(localUser);

            auditService
                    .logMessage("User with id " + localUser.getId() + " was deleted in IDP", Map.of(
                            "userId", localUser.getId()
                    ));
        }
    }
}
