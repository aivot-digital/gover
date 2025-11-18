package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.dtos.DepartmentResponseDTO;
import de.aivot.GoverBackend.department.filters.DepartmentFilter;
import de.aivot.GoverBackend.department.filters.DepartmentWithMembershipFilter;
import de.aivot.GoverBackend.department.repositories.ShadowedOrganizationalUnitRepository;
import de.aivot.GoverBackend.department.services.DepartmentService;
import de.aivot.GoverBackend.department.services.DepartmentWithMembershipService;
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

@RestController
@RequestMapping("/api/organizational-units-shadowed/")
public class ShadowedOrganizationalUnitController {
    private final DepartmentService departmentService;
    private final DepartmentWithMembershipService departmentWithMembershipService;
    private final ShadowedOrganizationalUnitRepository shadowedOrganizationalUnitRepository;

    @Autowired
    public ShadowedOrganizationalUnitController(DepartmentService departmentService,
                                                DepartmentWithMembershipService departmentWithMembershipService,
                                                ShadowedOrganizationalUnitRepository shadowedOrganizationalUnitRepository) {

        this.departmentService = departmentService;
        this.departmentWithMembershipService = departmentWithMembershipService;
        this.shadowedOrganizationalUnitRepository = shadowedOrganizationalUnitRepository;
    }

    @GetMapping("")
    public Page<DepartmentResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid DepartmentWithMembershipFilter filter
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // If a user id is set in the filter, return the departments the user is a member of.
        if (filter.getUserId() != null) {
            return departmentWithMembershipService
                    .list(pageable, filter)
                    .map(DepartmentResponseDTO::fromEntity);
        }

        // If the user is a global admin, return all departments.
        if (user.getGlobalAdmin() || Boolean.TRUE.equals(filter.getIgnoreMemberships())) {
            var depFilter = DepartmentFilter
                    .create()
                    .setId(filter.getDepartmentId())
                    .setName(filter.getDepartmentName())
                    .setThemeId(filter.getThemeId());

            return shadowedOrganizationalUnitRepository
                    .findAll(depFilter.buildForShadowed(), pageable)
                    .map(DepartmentResponseDTO::fromEntity);
        }
        // If the user is not an admin, return only the departments the user is a member of.
        else {

            // If no user id is set in the filter, set the user id to the id of the current user.
            if (filter.getUserId() == null) {
                filter.setUserId(user.getId());
            }

            return departmentWithMembershipService
                    .list(pageable, filter)
                    .map(DepartmentResponseDTO::fromEntity);
        }
    }

    /**
     * Retrieve a department by its id.
     *
     * @param id The id of the department.
     * @return The department.
     */
    @GetMapping("{id}/")
    public DepartmentResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var department = departmentService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        return DepartmentResponseDTO
                .fromEntity(department);
    }
}
