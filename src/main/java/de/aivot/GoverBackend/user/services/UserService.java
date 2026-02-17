package de.aivot.GoverBackend.user.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.lib.services.EntityService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.models.KeycloakUser;
import de.aivot.GoverBackend.user.repositories.UserRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements EntityService<UserEntity, String> {
    private final KeyCloakApiService keyCloakApiService;
    private final UserRepository userRepository;

    @Autowired
    public UserService(
            KeyCloakApiService keyCloakApiService,
            UserRepository userRepository
    ) {
        this.keyCloakApiService = keyCloakApiService;
        this.userRepository = userRepository;
    }

    @Nonnull
    public Optional<UserEntity> fromJWT(
            @Nullable Jwt jwt
    ) throws ResponseException {
        if (jwt == null) {
            return Optional.empty();
        }

        var id = getIdFromJWT(jwt);
        if (id == null) {
            return Optional.empty();
        }

        var stored = retrieve(id);
        if (stored.isPresent()) {
            return stored;
        }

        return importUserFromKeycloak(id);
    }

    public UserEntity fromJWTOrThrow(
            @Nullable Jwt jwt
    ) throws ResponseException {
        return fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);
    }


    @Nullable
    public static String getIdFromJWT(
            @Nullable Jwt jwt
    ) {
        if (jwt == null) {
            return null;
        }

        return jwt.getClaimAsString("sub");
    }

    @Nonnull
    @Override
    public UserEntity create(@Nonnull UserEntity entity) throws ResponseException {
        var keycloakUserToCreate = KeycloakUser
                .from(entity);

        if (userRepository.existsByEmail(entity.getEmail())) {
            throw ResponseException
                    .badRequest("Es existiert bereits eine Mitarbeiter:in mit dieser E-Mail-Adresse.");
        }

        var createdKeycloakUser = keyCloakApiService
                .createUser(keycloakUserToCreate);

        var createdUserEntity = UserEntity
                .from(createdKeycloakUser)
                .setSystemRoleId(entity.getSystemRoleId());

        return userRepository.save(createdUserEntity);
    }


    @Nonnull
    @Override
    public Page<UserEntity> performList(@Nonnull Pageable pageable,
                                        @Nullable Specification<UserEntity> specification,
                                        @Nullable Filter<UserEntity> filter
    ) throws ResponseException {
        return userRepository
                .findAll(specification, pageable);
    }

    @Nonnull
    @Override
    public Optional<UserEntity> retrieve(@Nonnull String id) throws ResponseException {
        return userRepository.findById(id);
    }

    @Nonnull
    public Optional<UserEntity> importUserFromKeycloak(@Nonnull String id) throws ResponseException {
        // Try to get user from Keycloak
        var keycloakUser = keyCloakApiService
                .retrieveUser(id)
                .orElse(null);

        // If the user is not in Keycloak, return empty optional
        if (keycloakUser == null) {
            return Optional.empty();
        }

        // Create user entity from Keycloak user and roles
        var userFromKeycloak = UserEntity
                .from(keycloakUser);

        // Save user to database
        userRepository.save(userFromKeycloak);

        // Return user
        return Optional.of(userFromKeycloak);
    }

    @Nonnull
    @Override
    public Optional<UserEntity> retrieve(@Nonnull Specification<UserEntity> specification) {
        return userRepository.findOne(specification);
    }

    @Nonnull
    @Override
    public UserEntity performUpdate(@Nonnull String id,
                                    @Nonnull UserEntity entity,
                                    @Nonnull UserEntity existingEntity) throws ResponseException {


        var keycloakUserToUpdate = KeycloakUser
                .from(existingEntity)
                .setEmail(entity.getEmail())
                .setFirstName(entity.getFirstName())
                .setLastName(entity.getLastName());

        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt) {
            var execUser = fromJWT(jwt)
                    .orElseThrow(ResponseException::unauthorized);
            if (execUser.getIsSuperAdmin()) {
                keycloakUserToUpdate.setEnabled(entity.getEnabled());
            }
        }

        var updatedKeycloakUser = keyCloakApiService
                .updateUser(id, keycloakUserToUpdate);

        var userToUpdate = UserEntity
                .from(updatedKeycloakUser);

        if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof Jwt jwt) {
            var execUser = fromJWT(jwt)
                    .orElseThrow(ResponseException::unauthorized);
            if (execUser.getIsSuperAdmin()) {
                userToUpdate.setSystemRoleId(entity.getSystemRoleId());
            }
        }

        return userRepository.save(userToUpdate);
    }

    @Override
    public boolean exists(@Nonnull String id) {
        return userRepository.existsById(id);
    }

    @Override
    public boolean exists(@Nonnull Specification<UserEntity> specification) {
        return userRepository.exists(specification);
    }

    @Override
    public void performDelete(@Nonnull UserEntity entity) throws ResponseException {
        keyCloakApiService
                .deleteUser(entity.getId());

        entity.clearPersonalData();
        entity.setDeletedInIdp(true);
        entity.setEnabled(false);

        userRepository.save(entity);
    }

    public UserEntity updatePassword(String id, String password) throws ResponseException {
        var user = retrieve(id)
                .orElseThrow(() -> ResponseException.notFound("Mitarbeiter:in nicht gefunden"));

        return user;
    }
}
