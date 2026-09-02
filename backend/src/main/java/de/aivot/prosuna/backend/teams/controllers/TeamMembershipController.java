package de.aivot.prosuna.backend.teams.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.teams.dtos.TeamMembershipCreateRequestDTO;
import de.aivot.prosuna.backend.teams.entities.TeamMembershipEntity;
import de.aivot.prosuna.backend.teams.filters.TeamMembershipFilter;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.teams.services.TeamMembershipService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.utils.StringUtils;
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
@RequestMapping("/api/team-memberships/")
@Tag(
        name = OpenApiConstants.Tags.TeamMembershipsName,
        description = OpenApiConstants.Tags.TeamMembershipsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class TeamMembershipController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final TeamMembershipService teamMembershipService;
    private final PermissionService permissionService;

    @Autowired
    public TeamMembershipController(AuditService auditService,
                                    UserService userService,
                                    TeamMembershipService teamMembershipService,
                                    PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(TeamMembershipController.class, "Teams");
        this.userService = userService;
        this.teamMembershipService = teamMembershipService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team Memberships",
            description = "Retrieve a paginated list of team memberships. " +
                    "You can filter the results using various criteria."
    )
    public Page<TeamMembershipEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid TeamMembershipFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), TeamPermissionProvider.TEAM_MEMBERSHIP_READ)) {
            if (filter.getTeamId() != null) {
                permissionService.requireTeamPermission(
                        user.getId(),
                        filter.getTeamId(),
                        TeamPermissionProvider.TEAM_MEMBERSHIP_READ
                );
            } else {
                var accessibleTeamIds = permissionService
                        .getTeamsWithPermission(user.getId(), TeamPermissionProvider.TEAM_MEMBERSHIP_READ);

                if (filter.getTeamIds() != null) {
                    accessibleTeamIds = filter.getTeamIds()
                            .stream()
                            .filter(accessibleTeamIds::contains)
                            .toList();
                }

                if (accessibleTeamIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setTeamIds(accessibleTeamIds);
            }
        }

        return teamMembershipService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Team Membership",
            description = "Create a new team membership to assign a user to a team. " +
                    "Requires the permission `" + TeamPermissionProvider.TEAM_MEMBERSHIP_CREATE +
                    "` for the target team or at system level."
    )
    public TeamMembershipEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid TeamMembershipCreateRequestDTO createDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireTeamPermission(
                execUser.getId(),
                createDTO.teamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_CREATE
        );

        var roleIds = createDTO.roleIdsOrEmpty();

        var result = teamMembershipService
                .createWithRoles(createDTO.toEntity(), roleIds);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Create, TeamMembershipEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "teamId", result.getTeamId(),
                "userId", result.getUserId(),
                "roleIds", roleIds
        )).withMessage(
                "Die Teamzugehörigkeit mit der ID %s für das Team %s und die Mitarbeiter:in %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getTeamId())),
                StringUtils.quote(result.getUserId()),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team Membership",
            description = "Retrieve a specific team membership by its ID."
    )
    public TeamMembershipEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var membership = teamMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireTeamPermission(
                user.getId(),
                membership.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_READ
        );

        return membership;
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Team Membership",
            description = "Update an existing team membership. " +
                    "Requires the permission `" + TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE +
                    "` for the membership's team or at system level."
    )
    public TeamMembershipEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid TeamMembershipEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var existing = teamMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireTeamPermission(
                execUser.getId(),
                existing.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_UPDATE
        );

        var result = teamMembershipService
                .update(id, updateDTO);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Update, TeamMembershipEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "teamId", result.getTeamId(),
                "userId", result.getUserId()
        )).withMessage(
                "Die Teamzugehörigkeit mit der ID %s für das Team %s und die Mitarbeiter:in %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(String.valueOf(result.getTeamId())),
                StringUtils.quote(result.getUserId()),
                StringUtils.quote(execUser.getFullName())
        ).log();

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Team Membership",
            description = "Delete a team membership by its ID. " +
                    "Requires the permission `" + TeamPermissionProvider.TEAM_MEMBERSHIP_DELETE +
                    "` for the membership's team or at system level."
    )
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var entity = teamMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireTeamPermission(
                execUser.getId(),
                entity.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_DELETE
        );

        var deleted = teamMembershipService
                .delete(id);

        auditService.create().withUser(execUser).withAuditAction(AuditAction.Delete, TeamMembershipEntity.class, deleted.getId(), "id", Map.of(
                "id", deleted.getId(),
                "teamId", deleted.getTeamId(),
                "userId", deleted.getUserId()
        )).withMessage(
                "Die Teamzugehörigkeit mit der ID %s für das Team %s und die Mitarbeiter:in %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(String.valueOf(deleted.getId())),
                StringUtils.quote(String.valueOf(deleted.getTeamId())),
                StringUtils.quote(deleted.getUserId()),
                StringUtils.quote(execUser.getFullName())
        ).log();
    }
}
