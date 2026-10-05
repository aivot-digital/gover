package de.aivot.prosuna.backend.teams.controllers;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
import de.aivot.prosuna.backend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.prosuna.backend.teams.filters.VTeamMembershipWithDetailsFilter;
import de.aivot.prosuna.backend.teams.permissions.TeamPermissionProvider;
import de.aivot.prosuna.backend.teams.services.VTeamMembershipWithDetailsService;
import de.aivot.prosuna.backend.user.services.UserService;
import de.aivot.prosuna.backend.userRoles.permissions.DomainRolePermissionProvider;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/team-memberships-with-details/")
@Tag(
        name = OpenApiConstants.Tags.TeamMembershipsName,
        description = OpenApiConstants.Tags.TeamMembershipsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VTeamMembershipWithDetailsController {

    private final VTeamMembershipWithDetailsService vTeamMembershipWithDetailsService;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public VTeamMembershipWithDetailsController(VTeamMembershipWithDetailsService vTeamMembershipWithDetailsService,
                                                PermissionService permissionService,
                                                UserService userService) {
        this.vTeamMembershipWithDetailsService = vTeamMembershipWithDetailsService;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team Memberships with Details",
            description = "Retrieve a paginated list of team memberships with detailed information. " +
                    "Supports filtering and pagination."
    )
    public Page<VTeamMembershipWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VTeamMembershipWithDetailsFilter filter
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

        var page = vTeamMembershipWithDetailsService
                .list(pageable, filter);

        if (!permissionService.hasSystemPermission(user.getId(), DomainRolePermissionProvider.DOMAIN_ROLE_READ)) {
            return page.map(VTeamMembershipWithDetailsController::redactDomainRoleDetails);
        }

        return page;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team Membership with Details",
            description = "Retrieve detailed information about a specific team membership by its ID."
    )
    public VTeamMembershipWithDetailsEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var membership = vTeamMembershipWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireTeamPermission(
                user.getId(),
                membership.getTeamId(),
                TeamPermissionProvider.TEAM_MEMBERSHIP_READ
        );

        if (!permissionService.hasSystemPermission(user.getId(), DomainRolePermissionProvider.DOMAIN_ROLE_READ)) {
            return redactDomainRoleDetails(membership);
        }

        return membership;
    }

    private static VTeamMembershipWithDetailsEntity redactDomainRoleDetails(VTeamMembershipWithDetailsEntity membership) {
        // Membership read permission grants access to the membership itself, but not to domain role metadata.
        return membership
                .setDomainRoles(List.of())
                .setDomainRoleAssignments(List.of())
                .setDomainRolePermissions(List.of());
    }
}
