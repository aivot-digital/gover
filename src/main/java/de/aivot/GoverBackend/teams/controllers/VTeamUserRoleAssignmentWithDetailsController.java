package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.teams.entities.VTeamUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.teams.filters.VTeamMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.teams.filters.VTeamUserRoleAssignmentWithDetailsFilter;
import de.aivot.GoverBackend.teams.services.VTeamMembershipWithDetailsService;
import de.aivot.GoverBackend.teams.services.VTeamMembershipWithPermissionsService;
import de.aivot.GoverBackend.teams.services.VTeamUserRoleAssignmentWithDetailsService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.services.UserRoleAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/team-user-role-assignments-with-details/")
@Tag(
        name = OpenApiConstants.Tags.TeamUserRoleAssignmentsWithDetails,
        description = OpenApiConstants.Tags.TeamUserRoleAssignmentsWithDetailsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VTeamUserRoleAssignmentWithDetailsController {
    private final ScopedAuditService auditService;

    private final VTeamUserRoleAssignmentWithDetailsService VTeamUserRoleAssignmentWithDetailsService;
    private final UserRoleAssignmentService userRoleAssignmentService;
    private final VTeamMembershipWithDetailsService VTeamMembershipWithDetailsService;
    private final VTeamMembershipWithPermissionsService vTeamMembershipWithPermissionsService;
    private final UserService userService;

    @Autowired
    public VTeamUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                        VTeamUserRoleAssignmentWithDetailsService VTeamUserRoleAssignmentWithDetailsService,
                                                        UserRoleAssignmentService userRoleAssignmentService,
                                                        VTeamMembershipWithDetailsService VTeamMembershipWithDetailsService,
                                                        VTeamMembershipWithPermissionsService vTeamMembershipWithPermissionsService,
                                                        UserService userService) {
        this.auditService = auditService.createScopedAuditService(VTeamUserRoleAssignmentWithDetailsController.class);

        this.VTeamUserRoleAssignmentWithDetailsService = VTeamUserRoleAssignmentWithDetailsService;
        this.userRoleAssignmentService = userRoleAssignmentService;
        this.VTeamMembershipWithDetailsService = VTeamMembershipWithDetailsService;
        this.vTeamMembershipWithPermissionsService = vTeamMembershipWithPermissionsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team User Role Assignments with Details",
            description = "Retrieve a paginated list of team user role assignments with detailed information. " +
                    "Supports filtering based on various criteria."
    )
    public Page<VTeamUserRoleAssignmentWithDetailsEntity> list(
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VTeamUserRoleAssignmentWithDetailsFilter filter
    ) throws ResponseException {
        return VTeamUserRoleAssignmentWithDetailsService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Team User Role Assignment",
            description = "Create a new user role assignment within a team membership. " +
                    "Requires super admin privileges or appropriate team edit permissions."
    )
    public VTeamUserRoleAssignmentWithDetailsEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleAssignmentEntity newAssignment
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        if (Objects.isNull(newAssignment.getTeamMembershipId())) {
            throw ResponseException.badRequest("Die ID der Teamzugehörigkeit muss angegeben werden.");
        }

        var membership = VTeamMembershipWithDetailsService
                .retrieve(newAssignment.getTeamMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getIsSuperAdmin()) {
            var filter = VTeamMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setTeamId(membership.getTeamId())
                    .setTeamPermissionEdit(true);

            var hasPermissionToEdit = vTeamMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.TeamPermissionEdit);
            }
        }

        var created = userRoleAssignmentService
                .create(newAssignment);

        auditService
                .logAction(user, AuditAction.Create, UserRoleAssignmentEntity.class, Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId()
                ));

        return VTeamUserRoleAssignmentWithDetailsService
                .retrieve(created.getId())
                .orElseThrow(ResponseException::notFound);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team User Role Assignment with Details",
            description = "Retrieve detailed information about a specific team user role assignment by its ID."
    )
    public VTeamUserRoleAssignmentWithDetailsEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return VTeamUserRoleAssignmentWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Team User Role Assignment",
            description = "Delete a user role assignment from a team membership. " +
                    "Requires super admin privileges or appropriate team edit permissions."
    )
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var entity = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(entity.getTeamMembershipId())) {
            throw ResponseException.badRequest("Die ID der Teamzugehörigkeit muss angegeben werden.");
        }

        var membership = VTeamMembershipWithDetailsService
                .retrieve(entity.getTeamMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getIsSuperAdmin()) {
            var filter = VTeamMembershipWithPermissionsFilter
                    .create()
                    .setUserId(user.getId())
                    .setTeamId(membership.getTeamId())
                    .setTeamPermissionEdit(true);

            var hasPermissionToEdit = vTeamMembershipWithPermissionsService
                    .exists(filter.build());

            if (!hasPermissionToEdit) {
                throw ResponseException
                        .noPermission(PermissionLabels.TeamPermissionEdit);
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
