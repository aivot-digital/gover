package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.filters.VDepartmentUserRoleAssignmentWithDetailsFilter;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.department.services.VDepartmentUserRoleAssignmentWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.services.UserRoleAssignmentService;
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
import java.util.Objects;

@RestController
@RequestMapping("/api/department-user-role-assignments-with-details/")
@Tag(
        name = "Department User Role Assignments",
        description = "User roles are assigned to users within the context of a department membership. " +
                "This allows for granular control over user permissions and access rights specific to each department."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VDepartmentUserRoleAssignmentWithDetailsController {
    private final ScopedAuditService auditService;

    private final VDepartmentUserRoleAssignmentWithDetailsService vDepartmentUserRoleAssignmentWithDetailsService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    private final UserService userService;
    private final DepartmentMembershipService departmentMembershipService;

    @Autowired
    public VDepartmentUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                              VDepartmentUserRoleAssignmentWithDetailsService vDepartmentUserRoleAssignmentWithDetailsService,
                                                              UserRoleAssignmentService userRoleAssignmentService,
                                                              VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService,
                                                              UserService userService,
                                                              DepartmentMembershipService departmentMembershipService) {
        this.auditService = auditService.createScopedAuditService(VDepartmentUserRoleAssignmentWithDetailsController.class, "Organisationseinheiten");

        this.vDepartmentUserRoleAssignmentWithDetailsService = vDepartmentUserRoleAssignmentWithDetailsService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
        this.userService = userService;
        this.departmentMembershipService = departmentMembershipService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Department User Role Assignments with Details",
            description = "Retrieve a paginated list of department user role assignments with detailed information. " +
                    "Supports filtering based on various criteria."
    )
    public Page<VDepartmentUserRoleAssignmentWithDetailsEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VDepartmentUserRoleAssignmentWithDetailsFilter filter
    ) throws ResponseException {
        return vDepartmentUserRoleAssignmentWithDetailsService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Department User Role Assignment",
            description = "Create a new user role assignment within a department membership. " +
                    "Requires super admin privileges or appropriate department edit permissions."
    )
    public VDepartmentUserRoleAssignmentWithDetailsEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleAssignmentEntity newAssignment
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (Objects.isNull(newAssignment.getDepartmentMembershipId())) {
            throw ResponseException.badRequest("Die ID der Abteilungszugehörigkeit muss angegeben werden.");
        }

        var membership = departmentMembershipService
                .retrieve(newAssignment.getDepartmentMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getIsSuperAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(membership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.DepartmentPermissionEdit);
            }
        }

        var created = userRoleAssignmentService
                .create(newAssignment);

        auditService.create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Create,
                        UserRoleAssignmentEntity.class,
                        created.getId(),
                        "id",
                        Map.of(
                                "userRoleId", created.getUserRoleId(),
                                "departmentMembershipId", created.getDepartmentMembershipId(),
                                "departmentId", membership.getDepartmentId(),
                                "userId", membership.getUserId()
                        ))
                .withMessage(
                        "Die Rollen-Zuweisung mit der ID %s zur Abteilungszugehörigkeit %s (Organisationseinheit %s, Mitarbeiter:in %s, Rolle %s) wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(String.valueOf(created.getId())),
                        StringUtils.quote(String.valueOf(created.getDepartmentMembershipId())),
                        StringUtils.quote(String.valueOf(membership.getDepartmentId())),
                        StringUtils.quote(membership.getUserId()),
                        StringUtils.quote(String.valueOf(created.getUserRoleId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return vDepartmentUserRoleAssignmentWithDetailsService
                .retrieve(created.getId())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Department User Role Assignment with Details",
            description = "Retrieve detailed information about a specific department user role assignment by its ID."
    )
    public VDepartmentUserRoleAssignmentWithDetailsEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return vDepartmentUserRoleAssignmentWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Department User Role Assignment",
            description = "Delete a user role assignment from a department membership. " +
                    "Requires super admin privileges or appropriate department edit permissions."
    )
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var entity = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(entity.getDepartmentMembershipId())) {
            throw ResponseException.badRequest("Die ID der Abteilungszugehörigkeit muss angegeben werden.");
        }

        var membership = departmentMembershipService
                .retrieve(entity.getDepartmentMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getIsSuperAdmin()) {
            var filter = VDepartmentMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setDepartmentId(membership.getDepartmentId())
                    .setDepartmentPermissionEdit(true);

            var hasPermissionToEdit = vDepartmentMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.DepartmentPermissionEdit);
            }
        }

        userRoleAssignmentService
                .deleteEntity(entity);

        auditService.create()
                .withUser(user)
                .withAuditAction(
                        AuditAction.Delete,
                        UserRoleAssignmentEntity.class,
                        entity.getId(),
                        "id",
                        Map.of(
                                "userRoleId", entity.getUserRoleId(),
                                "departmentMembershipId", entity.getDepartmentMembershipId(),
                                "departmentId", membership.getDepartmentId(),
                                "userId", membership.getUserId()
                        ))
                .withMessage(
                        "Die Rollen-Zuweisung mit der ID %s zur Abteilungszugehörigkeit %s (Organisationseinheit %s, Mitarbeiter:in %s, Rolle %s) wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(String.valueOf(entity.getId())),
                        StringUtils.quote(String.valueOf(entity.getDepartmentMembershipId())),
                        StringUtils.quote(String.valueOf(membership.getDepartmentId())),
                        StringUtils.quote(membership.getUserId()),
                        StringUtils.quote(String.valueOf(entity.getUserRoleId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();
    }
}
