package de.aivot.prosuna.backend.teams.controllers;

import de.aivot.prosuna.backend.audit.enums.AuditAction;
import de.aivot.prosuna.backend.audit.services.AuditService;
import de.aivot.prosuna.backend.audit.services.ScopedAuditService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.teams.entities.TeamEntity;
import de.aivot.prosuna.backend.teams.filters.TeamFilter;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.teams.services.TeamService;
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
@RequestMapping("/api/teams/")
@Tag(
        name = OpenApiConstants.Tags.TeamsName,
        description = OpenApiConstants.Tags.TeamsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class TeamController {
    private final ScopedAuditService auditService;
    private final UserService userService;
    private final TeamService teamService;
    private final PermissionService permissionService;

    @Autowired
    public TeamController(AuditService auditService,
                          UserService userService,
                          TeamService teamService,
                          PermissionService permissionService) {
        this.auditService = auditService.createScopedAuditService(TeamController.class, "Teams");

        this.userService = userService;
        this.teamService = teamService;
        this.permissionService = permissionService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Teams",
            description = "Retrieve a paginated list of teams. " +
                    "Supports filtering and pagination parameters."
    )
    public Page<TeamEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid TeamFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), TeamPermissionProvider.TEAM_READ)) {
            if (filter.getId() != null) {
                permissionService.requireTeamPermission(
                        user.getId(),
                        filter.getId(),
                        TeamPermissionProvider.TEAM_READ
                );
            } else {
                var accessibleTeamIds = permissionService
                        .getTeamsWithPermission(user.getId(), TeamPermissionProvider.TEAM_READ);

                if (filter.getIds() != null) {
                    // Preserve explicit client filtering, but intersect it with the teams the user may read.
                    accessibleTeamIds = filter.getIds()
                            .stream()
                            .filter(accessibleTeamIds::contains)
                            .toList();
                }

                if (accessibleTeamIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setIds(accessibleTeamIds);
            }
        }

        return teamService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Team",
            description = "Create a new team. Requires the system-level permission `" +
                    TeamPermissionProvider.TEAM_CREATE + "`."
    )
    public TeamEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid TeamEntity newTeam
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireSystemPermission(
                user.getId(),
                TeamPermissionProvider.TEAM_CREATE
        );

        var result = teamService
                .create(newTeam);

        auditService.create().withUser(user).withAuditAction(AuditAction.Create, TeamEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "name", result.getName()
        )).withMessage(
                "Das Team %s mit der ID %s wurde von der Mitarbeiter:in %s erstellt.",
                StringUtils.quote(result.getName()),
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(user.getFullName())
        ).log();

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team",
            description = "Retrieve a team by its ID."
    )
    public TeamEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireTeamPermission(
                user.getId(),
                id,
                TeamPermissionProvider.TEAM_READ
        );

        return teamService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Team",
            description = "Update an existing team. Requires the permission `" +
                    TeamPermissionProvider.TEAM_UPDATE + "` for the affected team or at system level."
    )
    public TeamEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid TeamEntity updateDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireTeamPermission(
                user.getId(),
                id,
                TeamPermissionProvider.TEAM_UPDATE
        );

        TeamEntity result;
        try {
            result = teamService
                    .update(id, updateDTO);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern des Teams", e);
        }

        auditService.create().withUser(user).withAuditAction(AuditAction.Update, TeamEntity.class, result.getId(), "id", Map.of(
                "id", result.getId(),
                "name", result.getName()
        )).withMessage(
                "Das Team %s mit der ID %s wurde von der Mitarbeiter:in %s aktualisiert.",
                StringUtils.quote(result.getName()),
                StringUtils.quote(String.valueOf(result.getId())),
                StringUtils.quote(user.getFullName())
        ).log();

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Team",
            description = "Delete a team by its ID. Requires the permission `" +
                    TeamPermissionProvider.TEAM_DELETE + "` for the affected team or at system level."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.requireTeamPermission(
                user.getId(),
                id,
                TeamPermissionProvider.TEAM_DELETE
        );

        var deleted = teamService
                .delete(id);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, TeamEntity.class, deleted.getId(), "id", Map.of(
                "id", deleted.getId(),
                "name", deleted.getName()
        )).withMessage(
                "Das Team %s mit der ID %s wurde von der Mitarbeiter:in %s gelöscht.",
                StringUtils.quote(deleted.getName()),
                StringUtils.quote(String.valueOf(deleted.getId())),
                StringUtils.quote(user.getFullName())
        ).log();
    }
}
