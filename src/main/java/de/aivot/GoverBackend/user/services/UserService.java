package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.ReadEntityService;
import de.aivot.GoverBackend.user.cache.entities.UserCacheEntity;
import de.aivot.GoverBackend.user.cache.repositories.UserCacheRepository;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements ReadEntityService<UserEntity, String> {
    private final KeyCloakApiService keyCloakApiService;
    private final UserRepository userRepository;
    private final UserCacheRepository userCacheRepository;

    @Autowired
    public UserService(
            KeyCloakApiService keyCloakApiService,
            UserRepository userRepository,
            UserCacheRepository userCacheRepository
    ) {
        this.keyCloakApiService = keyCloakApiService;
        this.userRepository = userRepository;
        this.userCacheRepository = userCacheRepository;
    }

    public static Optional<UserEntity> fromJWT(
            @Nullable Jwt jwt
    ) {
        if (jwt == null) {
            return Optional.empty();
        }

        var userEntity = UserEntity
                .from(jwt);

        return Optional
                .of(userEntity);
    }

    @Nonnull
    @Override
    public Page<UserEntity> performList(
            @Nonnull Pageable pageable,
            @Nullable Specification<UserEntity> specification,
            @Nullable Filter<UserEntity> filter
    ) throws ResponseException {
        return userRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<UserEntity> retrieve(@Nonnull String id) throws ResponseException {
        // Try to get user from cache
        var cachedUser = userCacheRepository
                .findById(id)
                .orElse(null);
        // If user is cached, return it
        if (cachedUser != null) {
            var user = UserEntity
                    .from(cachedUser);
            return Optional
                    .of(user);
        }

        // Try to get user from database
        var localUser = userRepository
                .findById(id)
                .orElse(null);

        // If user is in database, cache it and return it
        if (localUser != null) {
            userCacheRepository.save(UserCacheEntity.from(localUser));

            return Optional
                    .of(localUser);
        }

        return retrieveUserFromKeycloak(id);
    }

    @Nonnull
    public Optional<UserEntity> retrieveUserFromKeycloak(@Nonnull String id) throws ResponseException {
        // Try to get user from Keycloak
        var keycloakUser = keyCloakApiService
                .getUser(id)
                .orElse(null);

        // If the user is not in Keycloak, return empty optional
        if (keycloakUser == null) {
            return Optional.empty();
        }

        // Get user roles from Keycloak
        var keycloakRoles = keyCloakApiService
                .getRoles(id);

        // Create user entity from Keycloak user and roles
        var userFromKeycloak = UserEntity
                .from(keycloakUser, keycloakRoles);

        // Cache user
        userCacheRepository.save(UserCacheEntity.from(userFromKeycloak));

        // Save user to database
        userRepository.save(userFromKeycloak);

        // Return user
        return Optional.of(userFromKeycloak);
    }

    @Nonnull
    @Override
    public Optional<UserEntity> retrieve(@Nonnull Specification<UserEntity> specification) {
        return Optional.empty();
    }

    @Override
    public boolean exists(@Nonnull String id) {
        if (userCacheRepository.existsById(id)) {
            return true;
        }

        if (userRepository.existsById(id)) {
            userRepository.findById(id).ifPresent(user -> {
                userCacheRepository.save(UserCacheEntity.from(user));
            });
            return true;
        }

        Optional<KeycloakUser> keycloakUser;
        try {
            keycloakUser = keyCloakApiService
                    .getUser(id);
        } catch (ResponseException e) {
            return false;
        }

        if (keycloakUser.isEmpty()) {
            return false;
        }

        List<String> keycloakRoles;
        try {
            keycloakRoles = keyCloakApiService
                    .getRoles(id);
        } catch (ResponseException e) {
            throw new RuntimeException(e);
        }

        var userFromKeycloak = UserEntity
                .from(keycloakUser.get(), keycloakRoles);

        userCacheRepository.save(UserCacheEntity.from(userFromKeycloak));
        userRepository.save(userFromKeycloak);

        return true;
    }

    @Override
    public boolean exists(@Nonnull Specification<UserEntity> specification) {
        return false;
    }
}
