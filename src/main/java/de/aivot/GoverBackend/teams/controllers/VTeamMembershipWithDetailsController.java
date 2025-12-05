package de.aivot.GoverBackend.teams.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.openApi.OpenApiConstants;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithDetailsEntity;
import de.aivot.GoverBackend.teams.filters.VTeamMembershipWithDetailsFilter;
import de.aivot.GoverBackend.teams.services.VTeamMembershipWithDetailsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/team-memberships-with-details/")
@Tag(
        name = OpenApiConstants.Tags.TeamMembershipsName,
        description = OpenApiConstants.Tags.TeamMembershipsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VTeamMembershipWithDetailsController {

    private final VTeamMembershipWithDetailsService vTeamMembershipWithDetailsService;

    @Autowired
    public VTeamMembershipWithDetailsController(VTeamMembershipWithDetailsService vTeamMembershipWithDetailsService) {
        this.vTeamMembershipWithDetailsService = vTeamMembershipWithDetailsService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Team Memberships with Details",
            description = "Retrieve a paginated list of team memberships with detailed information. " +
                    "Supports filtering and pagination."
    )
    public Page<VTeamMembershipWithDetailsEntity> list(
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VTeamMembershipWithDetailsFilter filter
    ) throws ResponseException {
        return vTeamMembershipWithDetailsService
                .list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Team Membership with Details",
            description = "Retrieve detailed information about a specific team membership by its ID."
    )
    public VTeamMembershipWithDetailsEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return vTeamMembershipWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
