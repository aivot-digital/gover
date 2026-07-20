package de.aivot.gover.backend.identity.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.identity.dtos.IdentityProviderDetailsDTO;
import de.aivot.gover.backend.identity.dtos.IdentityProviderListDTO;
import de.aivot.gover.backend.identity.dtos.IdentityProviderPrepareDTO;
import de.aivot.gover.backend.identity.dtos.IdentityProviderRequestDTO;
import de.aivot.gover.backend.identity.entities.IdentityProviderEntity;
import de.aivot.gover.backend.identity.filters.IdentityProviderFilter;
import de.aivot.gover.backend.identity.permissions.IdentityProviderPermissionProvider;
import de.aivot.gover.backend.identity.services.IdentityProviderService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/identity-providers/")
@Tag(
        name = "Identity Providers",
        description = "Identity providers are used to authenticate citizens in the application. " +
                "They can be configured by systems administrators and linked to forms to enable user authentication. " +
                "Identity providers support OAuth2 and OpenID Connect protocols and provide mappings for user attributes."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class IdentityProviderController {
    private final ScopedAuditService auditService;

    private final IdentityProviderService identityProviderService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public IdentityProviderController(AuditService auditService,
                                      IdentityProviderService identityProviderService,
                                      UserService userService,
                                      PermissionService permissionService) {
        this.auditService = auditService
                .createScopedAuditService(IdentityProviderController.class, "Identitätsanbieter");

        this.identityProviderService = identityProviderService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Identity Providers",
            description = "Retrieves a paginated list of identity providers based on the provided filters. " +
                    "This requires the permission „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ + "“."
    )
    public Page<IdentityProviderListDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid IdentityProviderFilter filter
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ);

        return identityProviderService
                .list(pageable, filter)
                .map(IdentityProviderListDTO::from);
    }

    @PostMapping("prepare/")
    @Operation(
            summary = "Prepare Identity Provider",
            description = "Prepares an identity provider by validating the provided endpoint and retrieving necessary metadata. " +
                    "This requires the permission „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE + "“ or „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE + "“."
    )
    public IdentityProviderDetailsDTO prepare(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody IdentityProviderPrepareDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE) &&
                !permissionService.hasSystemPermission(user.getId(), IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE)) {
            throw ResponseException.forbidden(
                    "Sie benötigen die Berechtigung %s oder %s auf Systemebene.",
                    StringUtils.quote(IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE),
                    StringUtils.quote(IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE)
            );
        }

        var preparedEntity = identityProviderService
                .prepare(requestDTO.endpoint());

        return IdentityProviderDetailsDTO
                .from(preparedEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Identity Provider",
            description = "Creates a new identity provider with the provided configuration. " +
                    "This requires the permission „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE + "“."
    )
    public IdentityProviderDetailsDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody IdentityProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), IdentityProviderPermissionProvider.IDENTITY_PROVIDER_CREATE);

        var created = identityProviderService
                .create(requestDTO.toEntity());

        auditService.create().withUser(user).withAuditAction(AuditAction.Create, IdentityProviderEntity.class, created.getKey(), "key", Map.of(
                "key", created.getKey(),
                "name", created.getName()
        )).withMessage(
                "Der Identitätsanbieter %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(created.getName()),
                StringUtils.quote(String.valueOf(created.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();

        return IdentityProviderDetailsDTO
                .from(created);
    }

    @GetMapping("{key}/")
    @Operation(
            summary = "Retrieve Identity Provider",
            description = "Retrieves the details of a specific identity provider by its unique key. " +
                    "This requires the permission „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ + "“."
    )
    public IdentityProviderDetailsDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, IdentityProviderPermissionProvider.IDENTITY_PROVIDER_READ);

        return identityProviderService
                .retrieve(key)
                .map(IdentityProviderDetailsDTO::from)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{key}/")
    @Operation(
            summary = "Update Identity Provider",
            description = "Updates the configuration of an existing identity provider. " +
                    "If the provider is disabled, it will be unlinked from all forms that use it. " +
                    "This requires the permission „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE + "“."
    )
    public IdentityProviderDetailsDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key,
            @Nonnull @RequestBody @Valid IdentityProviderRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), IdentityProviderPermissionProvider.IDENTITY_PROVIDER_UPDATE);

        // TODO: Check if the identity provide ris used in process node configs and prevent the disabling if so.

        var updatedEntity = identityProviderService
                .update(key, requestDTO.toEntity());

        auditService.create().withUser(user).withAuditAction(AuditAction.Update, IdentityProviderEntity.class, updatedEntity.getKey(), "key", Map.of(
                "key", updatedEntity.getKey(),
                "name", updatedEntity.getName()
        )).withMessage(
                "Der Identitätsanbieter %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(updatedEntity.getName()),
                StringUtils.quote(String.valueOf(updatedEntity.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();

        return IdentityProviderDetailsDTO
                .from(updatedEntity);
    }

    @DeleteMapping("{key}/")
    @Operation(
            summary = "Delete Identity Provider",
            description = "Deletes an identity provider if it is disabled and not linked to any published forms. " +
                    "This requires the permission „" + IdentityProviderPermissionProvider.IDENTITY_PROVIDER_DELETE + "“."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable UUID key
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), IdentityProviderPermissionProvider.IDENTITY_PROVIDER_DELETE);

        var entity = identityProviderService
                .retrieve(key)
                .orElseThrow(ResponseException::notFound);

        if (entity.getIsEnabled()) {
            throw ResponseException.conflict(
                    "Der Nutzerkontenanbieter %s kann nicht gelöscht werden, da er aktiviert ist.",
                    key
            );
        }

        // TODO: Check of this identity provider is still used in a process node config

        var deletedEntity = identityProviderService
                .delete(key);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, IdentityProviderEntity.class, deletedEntity.getKey(), "key", Map.of(
                "key", deletedEntity.getKey(),
                "name", deletedEntity.getName()
        )).withMessage(
                "Der Identitätsanbieter %s mit dem Schlüssel %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(deletedEntity.getName()),
                StringUtils.quote(String.valueOf(deletedEntity.getKey())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}
