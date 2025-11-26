package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentShadowedFilter;
import de.aivot.GoverBackend.department.services.VDepartmentShadowedService;
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
@RequestMapping("/api/departments-shadowed/")
public class VOrganizationalUnitShadowedController {
    /**
     * Service for handling shadowed organizational unit operations.
     */
    private final VDepartmentShadowedService vDepartmentShadowedService;

    /**
     * Constructs the controller with the required service.
     *
     * @param vDepartmentShadowedService the service for shadowed organizational units
     */
    @Autowired
    public VOrganizationalUnitShadowedController(VDepartmentShadowedService vDepartmentShadowedService) {
        this.vDepartmentShadowedService = vDepartmentShadowedService;
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
    public Page<VDepartmentShadowedEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid VDepartmentShadowedFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vDepartmentShadowedService
                .list(pageable, filter);
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
    public VDepartmentShadowedEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return vDepartmentShadowedService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
