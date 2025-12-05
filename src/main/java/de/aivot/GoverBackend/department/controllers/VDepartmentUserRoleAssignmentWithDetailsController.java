package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.filters.VDepartmentUserRoleAssignmentWithDetailsFilter;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithDetailsService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.department.services.VDepartmentUserRoleAssignmentWithDetailsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.services.UserRoleAssignmentService;
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
    private final VDepartmentMembershipWithDetailsService vDepartmentMembershipWithDetailsService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    private final UserService userService;

    @Autowired
    public VDepartmentUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                              VDepartmentUserRoleAssignmentWithDetailsService vDepartmentUserRoleAssignmentWithDetailsService,
                                                              UserRoleAssignmentService userRoleAssignmentService,
                                                              VDepartmentMembershipWithDetailsService vDepartmentMembershipWithDetailsService,
                                                              VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService,
                                                              UserService userService) {
        this.auditService = auditService.createScopedAuditService(VDepartmentUserRoleAssignmentWithDetailsController.class);

        this.vDepartmentUserRoleAssignmentWithDetailsService = vDepartmentUserRoleAssignmentWithDetailsService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.vDepartmentMembershipWithDetailsService = vDepartmentMembershipWithDetailsService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
        this.userService = userService;
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

        var membership = vDepartmentMembershipWithDetailsService
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

        auditService
                .logAction(user, AuditAction.Create, UserRoleAssignmentEntity.class, Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId()
                ));

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

        var membership = vDepartmentMembershipWithDetailsService
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

        auditService
                .logAction(user, AuditAction.Delete, UserRoleAssignmentEntity.class, Map.of(
                        "id", entity.getId(),
                        "userRoleId", entity.getUserRoleId()
                ));
    }
}

