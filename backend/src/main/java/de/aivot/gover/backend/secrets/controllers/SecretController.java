package de.aivot.gover.backend.secrets.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.exceptions.BadRequestException;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.secrets.dtos.SecretEntityRequestDTO;
import de.aivot.gover.backend.secrets.dtos.SecretEntityResponseDTO;
import de.aivot.gover.backend.secrets.entities.SecretEntity;
import de.aivot.gover.backend.secrets.filters.SecretFilter;
import de.aivot.gover.backend.secrets.permissions.SecretPermissionProvider;
import de.aivot.gover.backend.secrets.services.SecretService;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * This controller is responsible for handling requests to the secrets API.
 * A secret is used to store sensitive information like passwords, API keys, etc.
 */
@RestController
@RequestMapping("/api/secrets/")
@Tag(
        name = "Secrets",
        description = "Secrets are used to store sensitive information like passwords, API keys, etc. " +
                      "They are encrypted and can be retrieved securely."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SecretController {
    private final ScopedAuditService auditService;
    private final SecretService secretService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public SecretController(AuditService auditService,
                            SecretService secretService,
                            UserService userService,
                            PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(SecretController.class, "Geheimnisse");
        this.secretService = secretService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Secrets",
            description = "Retrieve a paginated list of secrets. " +
                          "Supports filtering by various criteria. " +
                          "Requires the system-level permission `" + SecretPermissionProvider.SECRET_READ + "`."
    )
    public Page<SecretEntityResponseDTO> list(
            @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid SecretFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), SecretPermissionProvider.SECRET_READ);

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
    @Operation(
            summary = "Create Secret",
            description = "Create a new secret. The secret is encrypted and stored securely. " +
                          "The response contains the key of the created secret. " +
                          "Requires the system-level permission `" + SecretPermissionProvider.SECRET_CREATE + "`."
    )
    public SecretEntityResponseDTO create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid SecretEntityRequestDTO secretDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), SecretPermissionProvider.SECRET_CREATE);

        // Save the secret with the authenticated user
        SecretEntity result = null;
        try {
            result = secretService
                    .create(secretDTO.toEntity());
        } catch (ResponseException e) {
            throw new BadRequestException(e);
        }

        // Log the action of creating a secret
        auditService.create().withUser(user).withAuditAction(AuditAction.Create, SecretEntity.class, result.getKey(), "key", Map.of("key", result.getKey())).withMessage(
                "Das Geheimnis %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(result.getName()),
                StringUtils.quote(String.valueOf(result.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();

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
    @Operation(
            summary = "Retrieve Secret",
            description = "Retrieve a secret by its unique key. " +
                          "The response contains the secret data excluding the encrypted value. " +
                          "Requires the system-level permission `" + SecretPermissionProvider.SECRET_READ + "`."
    )
    public SecretEntityResponseDTO retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), SecretPermissionProvider.SECRET_READ);

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
    @Operation(
            summary = "Update Secret",
            description = "Update an existing secret identified by its unique key. " +
                          "To preserve the old value, set the new value to a string of asterisks. " +
                          "Requires the system-level permission `" + SecretPermissionProvider.SECRET_UPDATE + "`."
    )
    public SecretEntityResponseDTO update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID key,
            @RequestBody @Valid SecretEntityRequestDTO secretDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), SecretPermissionProvider.SECRET_UPDATE);

        // Save the updated secret
        var result = secretService
                .update(key, secretDTO.toEntity());

        // Log the action of updating a secret
        auditService.create().withUser(user).withAuditAction(AuditAction.Update, SecretEntity.class, result.getKey(), "key", Map.of("key", result.getKey())).withMessage(
                "Das Geheimnis %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(result.getName()),
                StringUtils.quote(String.valueOf(result.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();

        return SecretEntityResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Secret",
            description = "Delete an existing secret identified by its unique key. " +
                          "Requires the system-level permission `" + SecretPermissionProvider.SECRET_DELETE + "`."
    )
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), SecretPermissionProvider.SECRET_DELETE);

        var entity = secretService
                .delete(key);

        // Log the action of deleting a secret
        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, SecretEntity.class, key, "key", Map.of("key", key,
                "name", entity.getName(),
                "description", entity.getDescription()
        )).withMessage(
                "Das Geheimnis %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(entity.getName()),
                StringUtils.quote(String.valueOf(key)),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}
