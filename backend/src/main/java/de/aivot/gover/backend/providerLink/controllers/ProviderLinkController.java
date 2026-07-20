package de.aivot.gover.backend.providerLink.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.config.permissions.ConfigPermissionProvider;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.providerLink.dtos.ProviderLinkRequestDTO;
import de.aivot.gover.backend.providerLink.dtos.ProviderLinkResponseDTO;
import de.aivot.gover.backend.providerLink.entities.ProviderLink;
import de.aivot.gover.backend.providerLink.filters.ProviderLinkFilter;
import de.aivot.gover.backend.providerLink.services.ProviderLinkService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

@RestController
@RequestMapping("/api/provider-links/")
@Tag(
        name = "Provider Links",
        description = "Provider links can be used to link to external resources from within Gover. " +
                      "They can be managed by system administrators and are often used to provide links to documentation, support pages, or other relevant external sites."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ProviderLinkController {
    private final ScopedAuditService auditService;
    private final ProviderLinkService providerLinkService;
    private final UserService userService;
    private final PermissionService permissionService;

    @Autowired
    public ProviderLinkController(
            AuditService auditService,
            ProviderLinkService providerLinkService,
            UserService userService,
            PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(ProviderLinkController.class, "Schnittstellen");
        this.providerLinkService = providerLinkService;
        this.userService = userService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Provider Links",
            description = "List provider links with pagination and filtering. " +
                    "This requires the permission „" + ConfigPermissionProvider.SYSTEM_CONFIG_READ + "“."
    )
    public Page<ProviderLinkResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProviderLinkFilter filter
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);

        return listProviderLinks(pageable, filter);
    }

    @GetMapping("available/")
    @Operation(
            summary = "List Available Provider Links",
            description = "List provider links for the staff dashboard. This endpoint is available to authenticated staff users without system configuration permissions."
    )
    public Page<ProviderLinkResponseDTO> listAvailable(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProviderLinkFilter filter
    ) throws ResponseException {
        return listProviderLinks(pageable, filter);
    }

    private Page<ProviderLinkResponseDTO> listProviderLinks(
            @Nonnull Pageable pageable,
            @Nonnull ProviderLinkFilter filter
    ) throws ResponseException {
        return providerLinkService
                .list(pageable, filter)
                .map(ProviderLinkResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Provider Link",
            description = "Create a new provider link. " +
                    "This requires the permission „" + ConfigPermissionProvider.SYSTEM_CONFIG_CREATE + "“."
    )
    public ProviderLinkResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody ProviderLinkRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_CREATE);

        var entity = providerLinkService
                .create(requestDTO.toEntity());

        auditService.create().withUser(user).withAuditAction(AuditAction.Create, ProviderLink.class, entity.getId(), "id", Map.of(
                                "id", entity.getId(),
                                "text", entity.getText(),
                                "link", entity.getLink()
                        )).withMessage(
                "Der Anbieterlink %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(entity.getText()),
                StringUtils.quote(String.valueOf(entity.getId())),
                StringUtils.quote(user.getFullName())
        ).log();

        return ProviderLinkResponseDTO
                .fromEntity(entity);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Provider Link",
            description = "Retrieve a provider link by its ID. " +
                    "This requires the permission „" + ConfigPermissionProvider.SYSTEM_CONFIG_READ + "“."
    )
    public ProviderLinkResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        permissionService
                .requireSystemPermission(jwt, ConfigPermissionProvider.SYSTEM_CONFIG_READ);

        return providerLinkService
                .retrieve(id)
                .map(ProviderLinkResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Provider Link",
            description = "Update an existing provider link. " +
                    "This requires the permission „" + ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE + "“."
    )
    public ProviderLinkResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody ProviderLinkRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_UPDATE);

        var entity = providerLinkService
                .update(id, requestDTO.toEntity());

        auditService.create().withUser(user).withAuditAction(AuditAction.Update, ProviderLink.class, entity.getId(), "id", Map.of(
                        "id", entity.getId(),
                        "text", entity.getText(),
                        "link", entity.getLink()
                )).withMessage(
                "Der Anbieterlink %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(entity.getText()),
                StringUtils.quote(String.valueOf(entity.getId())),
                StringUtils.quote(user.getFullName())
        ).log();

        return ProviderLinkResponseDTO
                .fromEntity(entity);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Provider Link",
            description = "Delete a provider link by its ID. " +
                    "This requires the permission „" + ConfigPermissionProvider.SYSTEM_CONFIG_DELETE + "“."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .requireSystemPermission(user.getId(), ConfigPermissionProvider.SYSTEM_CONFIG_DELETE);

        var link = providerLinkService
                .delete(id);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, ProviderLink.class, link.getId(), "id", Map.of(
                        "id", link.getId(),
                        "text", link.getText(),
                        "link", link.getLink()
                )).withMessage(
                "Der Anbieterlink %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(link.getText()),
                StringUtils.quote(String.valueOf(link.getId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}
