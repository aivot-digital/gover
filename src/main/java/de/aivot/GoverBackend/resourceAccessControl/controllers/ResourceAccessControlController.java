package de.aivot.GoverBackend.resourceAccessControl.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.resourceAccessControl.dtos.ResourceAccessControlRequestDTO;
import de.aivot.GoverBackend.resourceAccessControl.dtos.ResourceAccessControlResponseDTO;
import de.aivot.GoverBackend.resourceAccessControl.entities.ResourceAccessControlEntity;
import de.aivot.GoverBackend.resourceAccessControl.filters.ResourceAccessControlFilter;
import de.aivot.GoverBackend.resourceAccessControl.services.ResourceAccessControlService;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;

// TODO: Rework this to work with the user role permissions for the owning department

@RestController
@RequestMapping("/api/resource-access-controls/")
@Tag(
        name = "Resource Access Controls",
        description = "Resource access controls are used to manage access to various resources within the application."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class ResourceAccessControlController {
    private final ScopedAuditService auditService;
    private final ResourceAccessControlService resourceAccessControlService;
    private final UserService userService;

    @Autowired
    public ResourceAccessControlController(AuditService auditService,
                                           ResourceAccessControlService resourceAccessControlService,
                                           UserService userService) {
        this.auditService = auditService
                .createScopedAuditService(ResourceAccessControlController.class);

        this.resourceAccessControlService = resourceAccessControlService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Resource Access Controls",
            description = "Retrieve a paginated list of resource access controls with optional filtering."
    )
    public Page<ResourceAccessControlResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid ResourceAccessControlFilter filter
    ) throws ResponseException {
        return resourceAccessControlService
                .list(pageable, filter)
                .map(ResourceAccessControlResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Resource Access Control",
            description = "Create a new resource access control. " +
                          "Requires super admin privileges."
    )
    public ResourceAccessControlResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid ResourceAccessControlRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var created = resourceAccessControlService
                .create(requestDTO.toEntity());

        auditService.logAction(execUser, AuditAction.Create, ResourceAccessControlEntity.class, Map.of(
                "id", created.getId()
        ));

        return ResourceAccessControlResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Resource Access Control",
            description = "Retrieve a specific resource access control by its ID."
    )
    public ResourceAccessControlResponseDTO retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return resourceAccessControlService
                .retrieve(id)
                .map(ResourceAccessControlResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Resource Access Control",
            description = "Update an existing resource access control. " +
                          "Requires super admin privileges."
    )
    public ResourceAccessControlResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid ResourceAccessControlRequestDTO requestDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var result = resourceAccessControlService
                .update(id, requestDTO.toEntity());

        auditService.logAction(user, AuditAction.Update, ResourceAccessControlEntity.class, Map.of(
                "id", result.getId()
        ));

        return ResourceAccessControlResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Resource Access Control",
            description = "Delete a resource access control by its ID. " +
                          "Requires super admin privileges."
    )
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var entity = resourceAccessControlService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        resourceAccessControlService
                .deleteEntity(entity);

        auditService.logAction(user, AuditAction.Delete, ResourceAccessControlEntity.class, Map.of(
                "id", entity.getId()
        ));
    }
}

