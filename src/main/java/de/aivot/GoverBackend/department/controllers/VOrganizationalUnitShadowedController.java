package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.dtos.OrganizationalUnitShadowedResponseDTO;
import de.aivot.GoverBackend.department.filters.VOrganizationalUnitShadowedFilter;
import de.aivot.GoverBackend.department.services.VOrganizationalUnitShadowedService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * REST controller for accessing shadowed organizational unit data.
 * <p>
 * Provides endpoints to list and retrieve shadowed organizational units, which may include inherited or shadowed fields from parent units.
 * All endpoints require authentication.
 * </p>
 */
@RestController
@RequestMapping("/api/organizational-units-shadowed/")
public class VOrganizationalUnitShadowedController {
    /**
     * Service for handling shadowed organizational unit operations.
     */
    private final VOrganizationalUnitShadowedService vOrganizationalUnitShadowedService;

    /**
     * Constructs the controller with the required service.
     *
     * @param vOrganizationalUnitShadowedService the service for shadowed organizational units
     */
    @Autowired
    public VOrganizationalUnitShadowedController(VOrganizationalUnitShadowedService vOrganizationalUnitShadowedService) {
        this.vOrganizationalUnitShadowedService = vOrganizationalUnitShadowedService;
    }

    /**
     * Lists all shadowed organizational units with optional filtering and pagination.
     *
     * @param jwt      the authenticated user's JWT (may be null)
     * @param pageable pagination information
     * @param filter   filter criteria for shadowed organizational units
     * @return a page of OrganizationalUnitResponseDTOs
     * @throws ResponseException if authentication fails or another error occurs
     */
    @GetMapping("")
    public Page<OrganizationalUnitShadowedResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VOrganizationalUnitShadowedFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vOrganizationalUnitShadowedService
                .list(pageable, filter)
                .map(OrganizationalUnitShadowedResponseDTO::fromEntity);
    }

    /**
     * Retrieves a single shadowed organizational unit by its ID.
     *
     * @param jwt the authenticated user's JWT (may be null)
     * @param id  the unique identifier of the organizational unit
     * @return the OrganizationalUnitResponseDTO for the specified unit
     * @throws ResponseException if authentication fails or the unit is not found
     */
    @GetMapping("{id}/")
    public OrganizationalUnitShadowedResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var department = vOrganizationalUnitShadowedService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        return OrganizationalUnitShadowedResponseDTO
                .fromEntity(department);
    }
}
