package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.teams.entities.TeamEntity;
import de.aivot.GoverBackend.teams.filters.TeamFilter;
import de.aivot.GoverBackend.teams.services.TeamService;
import de.aivot.GoverBackend.user.services.UserService;
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

    @Autowired
    public TeamController(AuditService auditService,
                          UserService userService,
                          TeamService teamService) {
        this.auditService = auditService.createScopedAuditService(TeamController.class);

        this.userService = userService;
        this.teamService = teamService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Teams",
            description = "Retrieve a paginated list of teams. " +
                    "Supports filtering and pagination parameters."
    )
    public Page<TeamEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid TeamFilter filter
    ) throws ResponseException {
        return teamService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Team",
            description = "Create a new team. Requires system admin privileges."
    )
    public TeamEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid TeamEntity newTeam
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSystemAdmin()
                .orElseThrow(ResponseException::noSystemAdminPermission);

        var result = teamService
                .create(newTeam);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Create, TeamEntity.class, Map.of(
                "id", result.getId(),
                "name", result.getName()
        )));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team",
            description = "Retrieve a team by its ID."
    )
    public TeamEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return teamService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Team",
            description = "Update an existing team. Requires super admin privileges."
    )
    public TeamEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid TeamEntity updateDTO
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        TeamEntity result;
        try {
            result = teamService
                    .update(id, updateDTO);
        } catch (Exception e) {
            throw ResponseException.badRequest("Fehler beim Speichern des Teams", e);
        }

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Update, TeamEntity.class, Map.of(
                "id", result.getId(),
                "name", result.getName()
        )));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Team",
            description = "Delete a team by its ID. Requires super admin privileges."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asSuperAdmin()
                .orElseThrow(ResponseException::forbidden);

        var deleted = teamService
                .delete(id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Delete, TeamEntity.class, Map.of(
                "id", deleted.getId(),
                "name", deleted.getName()
        )));
    }
}
