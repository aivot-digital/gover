package de.aivot.GoverBackend.providerLink.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.providerLink.dtos.ProviderLinkRequestDTO;
import de.aivot.GoverBackend.providerLink.dtos.ProviderLinkResponseDTO;
import de.aivot.GoverBackend.providerLink.entities.ProviderLink;
import de.aivot.GoverBackend.providerLink.filters.ProviderLinkFilter;
import de.aivot.GoverBackend.providerLink.services.ProviderLinkService;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
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

    @Autowired
    public ProviderLinkController(
            AuditService auditService,
            ProviderLinkService providerLinkService,
            UserService userService) {
        this.auditService = auditService.createScopedAuditService(ProviderLinkController.class);
        this.providerLinkService = providerLinkService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Provider Links",
            description = "List provider links with pagination and filtering."
    )
    public Page<ProviderLinkResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ProviderLinkFilter filter
    ) throws ResponseException {
        return providerLinkService
                .list(pageable, filter)
                .map(ProviderLinkResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Provider Link",
            description = "Create a new provider link. Requires system admin permissions."
    )
    public ProviderLinkResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody ProviderLinkRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var entity = providerLinkService
                .create(requestDTO.toEntity());

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Create, this.getClass().getSimpleName(), ProviderLink.class, "legacy", "legacy", Map.of(
                                "id", entity.getId(),
                                "text", entity.getText(),
                                "link", entity.getLink()
                        )));

        return ProviderLinkResponseDTO
                .fromEntity(entity);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Provider Link",
            description = "Retrieve a provider link by its ID."
    )
    public ProviderLinkResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return providerLinkService
                .retrieve(id)
                .map(ProviderLinkResponseDTO::fromEntity)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Provider Link",
            description = "Update an existing provider link. Requires system admin permissions."
    )
    public ProviderLinkResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody ProviderLinkRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var entity = providerLinkService
                .update(id, requestDTO.toEntity());

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), ProviderLink.class, "legacy", "legacy", Map.of(
                        "id", entity.getId(),
                        "text", entity.getText(),
                        "link", entity.getLink()
                )));

        return ProviderLinkResponseDTO
                .fromEntity(entity);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Provider Link",
            description = "Delete a provider link by its ID. Requires system admin permissions."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var link = providerLinkService
                .delete(id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(user).withAuditAction(AuditAction.Delete, this.getClass().getSimpleName(), ProviderLink.class, "legacy", "legacy", Map.of(
                        "id", link.getId(),
                        "text", link.getText(),
                        "link", link.getLink()
                )));
    }
}
