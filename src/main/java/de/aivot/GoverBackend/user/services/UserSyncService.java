package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.cache.entities.UserCacheEntity;
import de.aivot.GoverBackend.user.cache.repositories.UserCacheRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
public class UserSyncService {
    private final static int delayInMS = 1000 * 60 * 5; // 5 minutes

    private final ScopedAuditService auditService;

    private final UserRepository userRepository;
    private final UserCacheRepository userCacheRepository;
    private final KeyCloakApiService keycloakApiService;

    @Autowired
    public UserSyncService(
            AuditService auditService,
            UserRepository userRepository,
            UserCacheRepository userCacheRepository,
            KeyCloakApiService keycloakApiService
    ) {
        this.auditService = auditService.createScopedAuditService(UserSyncService.class);

        this.userRepository = userRepository;
        this.userCacheRepository = userCacheRepository;
        this.keycloakApiService = keycloakApiService;
    }

    @Scheduled(fixedDelay = delayInMS)
    public void syncUsers() {
        var localUsers = userRepository
                .findAllByDeletedInIdpIsFalse();

        Collection<KeycloakUser> keycloakUsers;
        try {
            keycloakUsers = keycloakApiService
                    .getUsers();
        } catch (ResponseException e) {
            throw new RuntimeException(e.getMessage() + " - " + e.getDetails(), e);
        }

        var updatedUserIds = new HashSet<String>();

        for (var keycloakUser : keycloakUsers) {
            List<String> roles;
            try {
                roles = keycloakApiService
                        .getRoles(keycloakUser.getId());
            } catch (ResponseException e) {
                throw new RuntimeException(e.getMessage() + " - " + e.getDetails(), e);
            }

            var userEntity = UserEntity
                    .from(keycloakUser, roles);
            userRepository
                    .save(userEntity);
            userCacheRepository
                    .save(UserCacheEntity.from(userEntity));

            updatedUserIds.add(userEntity.getId());
        }

        for (var localUser : localUsers) {
            if (updatedUserIds.contains(localUser.getId())) {
                continue;
            }

            localUser.clearPersonalData();
            localUser.setDeletedInIdp(true);
            localUser.setEnabled(false);

            userRepository
                    .save(localUser);
            userCacheRepository
                    .save(UserCacheEntity.from(localUser));

            auditService
                    .logMessage("User with id " + localUser.getId() + " was deleted in IDP", Map.of(
                            "userId", localUser.getId()
                    ));
        }
    }
}
