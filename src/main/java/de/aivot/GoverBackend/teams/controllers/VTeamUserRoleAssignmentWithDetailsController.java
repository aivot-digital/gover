package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.permissions.entities.VUserTeamPermissionEntityId;
import de.aivot.GoverBackend.permissions.repositories.VUserTeamPermissionRepository;
import de.aivot.GoverBackend.teams.services.TeamMembershipService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.GoverBackend.userRoles.filters.UserRoleAssignmentFilter;
import de.aivot.GoverBackend.userRoles.services.UserRoleAssignmentService;
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
import java.util.Objects;

@RestController
@RequestMapping("/api/team-user-role-assignments-with-details/")
@Tag(
        name = "Team User Role Assignments",
        description = "User roles are assigned to users within the context of a team membership. " +
                      "This allows for granular control over user permissions and access rights specific to each team."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VTeamUserRoleAssignmentWithDetailsController {
    private static final String TEAM_PERMISSION_UPDATE = "team.update";
    private static final String TEAM_MEMBERSHIP_PERMISSION_UPDATE = "team_membership.update";

    private final ScopedAuditService auditService;

    private final UserRoleAssignmentService userRoleAssignmentService;
    private final UserService userService;
    private final TeamMembershipService teamMembershipService;
    private final VUserTeamPermissionRepository vUserTeamPermissionRepository;

    @Autowired
    public VTeamUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                        UserRoleAssignmentService userRoleAssignmentService,
                                                        UserService userService,
                                                        TeamMembershipService teamMembershipService,
                                                        VUserTeamPermissionRepository vUserTeamPermissionRepository) {
        this.auditService = auditService.createScopedAuditService(VTeamUserRoleAssignmentWithDetailsController.class);

        this.userRoleAssignmentService = userRoleAssignmentService;
        this.userService = userService;
        this.teamMembershipService = teamMembershipService;
        this.vUserTeamPermissionRepository = vUserTeamPermissionRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team User Role Assignments with Details",
            description = "Retrieve a paginated list of team user role assignments with detailed information. " +
                          "Supports filtering based on various criteria."
    )
    public Page<UserRoleAssignmentEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid UserRoleAssignmentFilter filter
    ) throws ResponseException {
        filter
                .setTeamAssignment(true)
                .setOrgUnitAssignment(false);

        return userRoleAssignmentService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Team User Role Assignment",
            description = "Create a new user role assignment within a team membership. " +
                          "Requires super admin privileges or appropriate team edit permissions."
    )
    public UserRoleAssignmentEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid UserRoleAssignmentEntity newAssignment
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (Objects.isNull(newAssignment.getTeamMembershipId())) {
            throw ResponseException.badRequest("Die ID der Teamzugehörigkeit muss angegeben werden.");
        }

        var membership = teamMembershipService
                .retrieve(newAssignment.getTeamMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getIsSuperAdmin() && !hasTeamEditPermission(user.getId(), membership.getTeamId())) {
            throw ResponseException
                    .noPermission(PermissionLabels.TeamPermissionEdit);
        }

        newAssignment.setDepartmentMembershipId(null);
        var created = userRoleAssignmentService
                .create(newAssignment);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Create, UserRoleAssignmentEntity.class, Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId()
                )));

        return created;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team User Role Assignment with Details",
            description = "Retrieve detailed information about a specific team user role assignment by its ID."
    )
    public UserRoleAssignmentEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var assignment = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(assignment.getTeamMembershipId())) {
            throw ResponseException.notFound();
        }

        return assignment;
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
                .orElseThrow(ResponseException::unauthorized);

        var entity = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(entity.getTeamMembershipId())) {
            throw ResponseException.badRequest("Die ID der Teamzugehörigkeit muss angegeben werden.");
        }

        var membership = teamMembershipService
                .retrieve(entity.getTeamMembershipId())
                .orElseThrow(ResponseException::badRequest);

        if (!user.getIsSuperAdmin() && !hasTeamEditPermission(user.getId(), membership.getTeamId())) {
            throw ResponseException
                    .noPermission(PermissionLabels.TeamPermissionEdit);
        }

        userRoleAssignmentService
                .deleteEntity(entity);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Delete, UserRoleAssignmentEntity.class, Map.of(
                        "id", entity.getId(),
                        "userRoleId", entity.getUserRoleId()
                )));
    }

    private boolean hasTeamEditPermission(@Nonnull String userId, @Nonnull Integer teamId) {
        return vUserTeamPermissionRepository
                .findById(VUserTeamPermissionEntityId.of(userId, teamId))
                .map(entry -> entry.getPermissions().contains(TEAM_PERMISSION_UPDATE)
                              || entry.getPermissions().contains(TEAM_MEMBERSHIP_PERMISSION_UPDATE))
                .orElse(false);
    }
}
