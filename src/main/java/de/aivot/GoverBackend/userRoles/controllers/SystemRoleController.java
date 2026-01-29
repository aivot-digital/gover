package de.aivot.GoverBackend.userRoles.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.permissions.data.Permissions;
import de.aivot.GoverBackend.core.services.PermissionService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.userRoles.entities.SystemRoleEntity;
import de.aivot.GoverBackend.userRoles.filters.SystemRoleFilter;
import de.aivot.GoverBackend.userRoles.services.SystemRoleService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.services.UserRoleService;
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

@RestController
@RequestMapping("/api/system-roles/")
@Tag(
        name = OpenApiConstants.Tags.SystemRolesName,
        description = OpenApiConstants.Tags.SystemRolesDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SystemRoleController {
    private final ScopedAuditService auditService;
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final SystemRoleService systemRoleService;
    private final PermissionService permissionService;

    @Autowired
    public SystemRoleController(AuditService auditService,
                                UserRoleService userRoleService,
                                UserService userService,
                                SystemRoleService systemRoleService,
                                PermissionService permissionService) {
        this.auditService = auditService
                .createScopedAuditService(SystemRoleController.class);

        this.userRoleService = userRoleService;
        this.userService = userService;
        this.systemRoleService = systemRoleService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List System Roles",
            description = "Retrieve a paginated list of system roles. Supports filtering and pagination. " +
                    "This requires the permission „" + Permissions.SYSTEM_ROLE_READ + "“."
    )
    public Page<SystemRoleEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid SystemRoleFilter filter
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermissionThrows(execUser.getId(), Permissions.SYSTEM_ROLE_READ);

        return systemRoleService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create System Role",
            description = "Create a new system role. " +
                    "This requires the permission „" + Permissions.SYSTEM_ROLE_CREATE + "“."
    )
    public SystemRoleEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid SystemRoleEntity newEntity
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermissionThrows(execUser.getId(), Permissions.SYSTEM_ROLE_CREATE);

        var createdEntity = systemRoleService
                .create(newEntity);

        auditService
                .logAction(execUser, AuditAction.Create, SystemRoleEntity.class, Map.of(
                        "id", createdEntity.getId(),
                        "name", createdEntity.getName()
                ));

        return createdEntity;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve System Role",
            description = "Retrieve a system role by its ID. " +
                    "This requires the permission „" + Permissions.SYSTEM_ROLE_READ + "“."
    )
    public SystemRoleEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService
                .hasSystemPermissionThrows(execUser.getId(), Permissions.SYSTEM_ROLE_READ);

        return systemRoleService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update System Role",
            description = "Update an existing system role. " +
                    "This requires the permission „" + Permissions.SYSTEM_ROLE_UPDATE + "“."
    )
    public SystemRoleEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid SystemRoleEntity patchedEntity
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        permissionService
                .hasSystemPermissionThrows(execUser.getId(), Permissions.SYSTEM_ROLE_UPDATE);

        var updatedEntity = systemRoleService
                .update(id, patchedEntity);

        auditService
                .logAction(execUser, AuditAction.Update, SystemRoleEntity.class, Map.of(
                        "id", updatedEntity.getId(),
                        "name", updatedEntity.getName()
                ));

        return updatedEntity;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete System Role",
            description = "Delete a system role by its ID. " +
                    "This requires the permission „" + Permissions.SYSTEM_ROLE_DELETE + "“."
    )
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        permissionService
                .hasSystemPermissionThrows(execUser.getId(), Permissions.SYSTEM_ROLE_DELETE);

        var entity = userRoleService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        userRoleService
                .deleteEntity(entity);

        auditService
                .logAction(execUser, AuditAction.Delete, UserRoleEntity.class, Map.of(
                        "id", entity.getId(),
                        "name", entity.getName()
                ));
    }
}
