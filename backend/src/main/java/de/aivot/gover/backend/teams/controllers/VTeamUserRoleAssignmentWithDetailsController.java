package de.aivot.gover.backend.teams.controllers;

import de.aivot.gover.backend.audit.enums.AuditAction;
import de.aivot.gover.backend.audit.services.AuditService;
import de.aivot.gover.backend.audit.services.ScopedAuditService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.gover.backend.teams.repositories.TeamMembershipRepository;
import de.aivot.gover.backend.teams.services.TeamMembershipService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.userRoles.entities.UserRoleAssignmentEntity;
import de.aivot.gover.backend.userRoles.filters.UserRoleAssignmentFilter;
import de.aivot.gover.backend.userRoles.services.UserRoleAssignmentService;
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
    private final ScopedAuditService auditService;

    private final UserRoleAssignmentService userRoleAssignmentService;
    private final UserService userService;
    private final TeamMembershipService teamMembershipService;
    private final TeamMembershipRepository teamMembershipRepository;
    private final PermissionService permissionService;

    @Autowired
    public VTeamUserRoleAssignmentWithDetailsController(AuditService auditService,
                                                        UserRoleAssignmentService userRoleAssignmentService,
                                                        UserService userService,
                                                        TeamMembershipService teamMembershipService,
                                                        TeamMembershipRepository teamMembershipRepository,
                                                        PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(VTeamUserRoleAssignmentWithDetailsController.class, "Teams");

        this.userRoleAssignmentService = userRoleAssignmentService;
        this.userService = userService;
        this.teamMembershipService = teamMembershipService;
        this.teamMembershipRepository = teamMembershipRepository;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team User Role Assignments with Details",
            description = "Retrieve a paginated list of team user role assignments with detailed information. " +
                          "Supports filtering based on various criteria."
    )
    public Page<UserRoleAssignmentEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid UserRoleAssignmentFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        filter
                .setTeamAssignment(true)
                .setOrgUnitAssignment(false);

        if (!permissionService.hasSystemPermission(user.getId(), TeamPermissionProvider.TEAM_MEMBERSHIP_READ)) {
            if (filter.getTeamMembershipId() != null) {
                var membership = teamMembershipService
                        .retrieve(filter.getTeamMembershipId())
                        .orElseThrow(ResponseException::notFound);

                permissionService.requireTeamPermission(
                        user.getId(),
                        membership.getTeamId(),
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ
                );
            } else {
                var accessibleTeamIds = permissionService
                        .getTeamsWithPermission(user.getId(), TeamPermissionProvider.TEAM_MEMBERSHIP_READ);

                if (accessibleTeamIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                var accessibleMembershipIds = teamMembershipRepository
                        .findIdsByTeamIdIn(accessibleTeamIds);

                if (filter.getTeamMembershipIds() != null) {
                    accessibleMembershipIds = filter.getTeamMembershipIds()
                            .stream()
                            .filter(accessibleMembershipIds::contains)
                            .toList();
                }

                if (accessibleMembershipIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setTeamMembershipIds(accessibleMembershipIds);
            }
        }

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

        permissionService.requireTeamPermission(
                user.getId(),
                membership.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE
        );

        newAssignment.setDepartmentMembershipId(null);
        var created = userRoleAssignmentService
                .create(newAssignment);

        auditService.create().withUser(user).withAuditAction(AuditAction.Create, UserRoleAssignmentEntity.class, created.getId(), "id", Map.of(
                        "id", created.getId(),
                        "userRoleId", created.getUserRoleId(),
                        "teamMembershipId", created.getTeamMembershipId(),
                        "teamId", membership.getTeamId(),
                        "userId", membership.getUserId()
                )).withMessage(
                "Die Team-Rollenzuweisung mit der ID %s zur Teamzugehörigkeit %s (Team %s, Mitarbeiter:in %s, Rolle %s) wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(created.getId())),
                StringUtils.quote(String.valueOf(created.getTeamMembershipId())),
                StringUtils.quote(String.valueOf(membership.getTeamId())),
                StringUtils.quote(membership.getUserId()),
                StringUtils.quote(String.valueOf(created.getUserRoleId())),
                StringUtils.quote(user.getFullName())
        ).log();

        return created;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team User Role Assignment with Details",
            description = "Retrieve detailed information about a specific team user role assignment by its ID."
    )
    public UserRoleAssignmentEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var assignment = userRoleAssignmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (Objects.isNull(assignment.getTeamMembershipId())) {
            throw ResponseException.notFound();
        }

        var membership = teamMembershipService
                .retrieve(assignment.getTeamMembershipId())
                .orElseThrow(ResponseException::notFound);

        permissionService.requireTeamPermission(
                user.getId(),
                membership.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_READ
        );

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

        permissionService.requireTeamPermission(
                user.getId(),
                membership.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE
        );

        userRoleAssignmentService
                .deleteEntity(entity);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, UserRoleAssignmentEntity.class, entity.getId(), "id", Map.of(
                        "id", entity.getId(),
                        "userRoleId", entity.getUserRoleId(),
                        "teamMembershipId", entity.getTeamMembershipId(),
                        "teamId", membership.getTeamId(),
                        "userId", membership.getUserId()
                )).withMessage(
                "Die Team-Rollenzuweisung mit der ID %s zur Teamzugehörigkeit %s (Team %s, Mitarbeiter:in %s, Rolle %s) wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(entity.getId())),
                StringUtils.quote(String.valueOf(entity.getTeamMembershipId())),
                StringUtils.quote(String.valueOf(membership.getTeamId())),
                StringUtils.quote(membership.getUserId()),
                StringUtils.quote(String.valueOf(entity.getUserRoleId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }

}
