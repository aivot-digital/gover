package de.aivot.GoverBackend.userRoles.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleRequestDTO;
import de.aivot.GoverBackend.userRoles.dtos.UserRoleResponseDTO;
import de.aivot.GoverBackend.userRoles.entities.UserRoleEntity;
import de.aivot.GoverBackend.userRoles.filters.UserRoleFilter;
import de.aivot.GoverBackend.userRoles.services.UserRoleService;
import de.aivot.GoverBackend.utils.StringUtils;
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
@RequestMapping("/api/user-roles/")
@Tag(name = "User Roles", description = "Manage user roles")
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class UserRoleController {
    private final ScopedAuditService auditService;
    private final UserRoleService userRoleService;
    private final UserService userService;

    @Autowired
    public UserRoleController(AuditService auditService, UserRoleService userRoleService, UserService userService) {
        this.auditService = auditService
                .createScopedAuditService(UserRoleController.class, "Rollen");

        this.userRoleService = userRoleService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List User Roles",
            description = "Retrieve a paginated list of user roles. Supports filtering and pagination."
    )
    public Page<UserRoleResponseDTO> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid UserRoleFilter filter
    ) throws ResponseException {
        return userRoleService
                .list(pageable, filter)
                .map(UserRoleResponseDTO::fromEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create User Role",
            description = "Create a new user role. Requires super admin privileges."
    )
    public UserRoleResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var created = userRoleService
                .create(requestDTO.toEntity());

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, UserRoleEntity.class, created.getId(), "id", Map.of(
                        "id", created.getId(),
                        "name", created.getName()
                )).withMessage(
                "Die Fachrolle %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(created.getName()),
                StringUtils.quote(String.valueOf(created.getId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return UserRoleResponseDTO
                .fromEntity(created);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve User Role",
            description = "Retrieve a user role by its ID."
    )
    public UserRoleResponseDTO retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return userRoleService
                .retrieve(id)
                .map(UserRoleResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update User Role",
            description = "Update an existing user role. Requires super admin privileges."
    )
    public UserRoleResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid UserRoleRequestDTO requestDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::noSuperAdminPermission);

        var result = userRoleService
                .update(id, requestDTO.toEntity());

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, UserRoleEntity.class, result.getId(), "id", Map.of(
                        "id", result.getId(),
                        "name", result.getName()
                )).withMessage(
                "Die Fachrolle %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(result.getName()),
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return UserRoleResponseDTO
                .fromEntity(result);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete User Role",
            description = "Delete a user role by its ID. Requires super admin privileges."
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

        var entity = userRoleService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        userRoleService
                .deleteEntity(entity);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Delete, UserRoleEntity.class, entity.getId(), "id", Map.of(
                        "id", entity.getId(),
                        "name", entity.getName()
                )).withMessage(
                "Die Fachrolle %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(entity.getName()),
                StringUtils.quote(String.valueOf(entity.getId())),
                StringUtils.quote(execUser.getFullName())
        ).log();
    }
}
