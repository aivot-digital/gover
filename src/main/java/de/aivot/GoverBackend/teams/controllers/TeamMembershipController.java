package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.teams.entities.TeamMembershipEntity;
import de.aivot.GoverBackend.teams.filters.TeamMembershipFilter;
import de.aivot.GoverBackend.teams.services.TeamMembershipService;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
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

    @Autowired
    public TeamMembershipController(AuditService auditService,
                                    UserService userService,
                                    TeamMembershipService teamMembershipService) {
        this.auditService = auditService.createScopedAuditService(TeamMembershipController.class);
        this.userService = userService;
        this.teamMembershipService = teamMembershipService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team Memberships",
            description = "Retrieve a paginated list of team memberships. " +
                    "You can filter the results using various criteria."
    )
    public Page<TeamMembershipEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid TeamMembershipFilter filter
    ) throws ResponseException {
        return teamMembershipService
                .list(pageable, filter);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Team Membership",
            description = "Create a new team membership to assign a user to a team. " +
                    "You need the „" + PermissionLabels.TeamPermissionEdit + "“ permission for the team to create a membership."
    )
    public TeamMembershipEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestBody @Valid TeamMembershipEntity createDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = teamMembershipService
                .create(createDTO);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Create, this.getClass().getSimpleName(), TeamMembershipEntity.class, "legacy", "legacy", Map.of(
                "id", result.getId(),
                "teamId", result.getTeamId(),
                "userId", result.getUserId()
        )));

        return result;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team Membership",
            description = "Retrieve a specific team membership by its ID."
    )
    public TeamMembershipEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return teamMembershipService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{id}/")
    @Operation(
            summary = "Update Team Membership",
            description = "Update an existing team membership. " +
                    "You need the „" + PermissionLabels.TeamPermissionEdit + "“ permission for the team to update a membership."
    )
    public TeamMembershipEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @RequestBody @Valid TeamMembershipEntity updateDTO
    ) throws ResponseException {
        var execUser = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var result = teamMembershipService
                .update(id, updateDTO);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Update, this.getClass().getSimpleName(), TeamMembershipEntity.class, "legacy", "legacy", Map.of(
                "id", result.getId(),
                "teamId", result.getTeamId(),
                "userId", result.getUserId()
        )));

        return result;
    }

    @DeleteMapping("{id}/")
    @Operation(
            summary = "Delete Team Membership",
            description = "Delete a team membership by its ID. " +
                    "You need the „" + PermissionLabels.TeamPermissionEdit + "“ permission for the team to delete a membership."
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

        var deleted = teamMembershipService
                .delete(id);

        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.create().withUser(execUser).withAuditAction(AuditAction.Delete, this.getClass().getSimpleName(), TeamMembershipEntity.class, "legacy", "legacy", Map.of(
                "id", deleted.getId(),
                "teamId", deleted.getTeamId(),
                "userId", deleted.getUserId()
        )));
    }
}
