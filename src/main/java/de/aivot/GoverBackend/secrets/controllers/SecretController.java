package de.aivot.GoverBackend.secrets.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.secrets.dtos.SecretEntityRequestDTO;
import de.aivot.GoverBackend.secrets.dtos.SecretEntityResponseDTO;
import de.aivot.GoverBackend.secrets.entities.SecretEntity;
import de.aivot.GoverBackend.secrets.services.SecretService;
import de.aivot.GoverBackend.secrets.filters.SecretFilter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * This controller is responsible for handling requests to the secrets API.
 * A secret is used to store sensitive information like passwords, API keys, etc.
 */
@RestController
@RequestMapping("/api/secrets/")
public class SecretController {
    private final ScopedAuditService auditService;
    private final SecretService secretService;

    @Autowired
    public SecretController(
            AuditService auditService,
            SecretService secretService
    ) {
        this.auditService = auditService.createScopedAuditService(SecretController.class);
        this.secretService = secretService;
    }

    @GetMapping("")
    public Page<SecretEntityResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid SecretFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return secretService
                .list(pageable, filter)
                .map(SecretEntityResponseDTO::fromEntity);
    }

    /**
     * Create a new secret.
     * This secret is encrypted and stored in the database.
     * The key is generated and returned in the response.
     *
     * @param jwt       The JWT token of the authenticated user.
     * @param secretDTO The secret data to be stored.
     * @return The response containing the key of the created secret.
     */
    @PostMapping("")
    public SecretEntityResponseDTO create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SecretEntityRequestDTO secretDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        // Save the secret with the authenticated user
        SecretEntity result = null;
        try {
            result = secretService
                    .create(secretDTO.toEntity());
        } catch (ResponseException e) {
            throw new BadRequestException(e);
        }

        // Log the action of creating a secret
        auditService.logAction(user, AuditAction.Create, SecretEntity.class, Map.of("key", result.getKey()));

        // Construct and return the response
        return SecretEntityResponseDTO.fromEntity(result);
    }

    /**
     * Retrieve a secret by key.
     * The key is used to uniquely identify the secret.
     * The response contains the secret data but not the encrypted value.
     * The response is returned as a {@link SecretEntityResponseDTO}.
     *
     * @param jwt The JWT token of the authenticated user.
     * @param key The key of the secret to be retrieved.
     * @return The response containing the secret data.
     */
    @GetMapping("{key}/")
    public SecretEntityResponseDTO retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String key
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return secretService
                .retrieve(key)
                .map(SecretEntityResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Update a secret by key.
     * The key is used to uniquely identify the secret.
     * The secret data is updated with the new values.
     * To preserve the old value, the new value can be set to a string of asterisks.
     * The response contains the updated secret data.
     * The response is returned as a {@link SecretEntityResponseDTO}.
     *
     * @param jwt       The JWT token of the authenticated user.
     * @param key       The key of the secret to be updated.
     * @param secretDTO The new secret data to be stored.
     * @return The response containing the updated secret data.
     */
    @PutMapping("{key}/")
    public SecretEntityResponseDTO update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String key,
            @RequestBody @Valid SecretEntityRequestDTO secretDTO
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        // Save the updated secret
        var result = secretService
                .update(key, secretDTO.toEntity());

        // Log the action of updating a secret
        auditService.logAction(user, AuditAction.Update, SecretEntity.class, Map.of("key", result.getKey()));

        return SecretEntityResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{key}/")
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String key
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = secretService
                .delete(key);

        // Log the action of deleting a secret
        auditService.logAction(user, AuditAction.Delete, SecretEntity.class, Map.of(
                "key", key,
                "name", entity.getName(),
                "description", entity.getDescription()
        ));

    }
}
