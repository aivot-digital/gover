package de.aivot.prosuna.backend.department.controllers;

import de.aivot.prosuna.backend.department.entities.VDepartmentMembershipWithDetailsEntity;
import de.aivot.prosuna.backend.department.filters.VDepartmentMembershipWithDetailsFilter;
import de.aivot.prosuna.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.prosuna.backend.department.services.VDepartmentMembershipWithDetailsService;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.openApi.OpenApiConfiguration;
import de.aivot.prosuna.backend.openApi.OpenApiConstants;
import de.aivot.prosuna.backend.permissions.services.PermissionService;
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
@RequestMapping("/api/department-memberships-with-details/")
@Tag(
        name = OpenApiConstants.Tags.DepartmentMembershipsName,
        description = OpenApiConstants.Tags.DepartmentMembershipsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VDepartmentMembershipWithDetailsController {
    private final VDepartmentMembershipWithDetailsService service;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public VDepartmentMembershipWithDetailsController(VDepartmentMembershipWithDetailsService service,
                                                      PermissionService permissionService,
                                                      UserService userService) {
        this.service = service;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Department Memberships with Details",
            description = "Retrieves a paginated list of department memberships along with detailed information about each membership. " +
                    "Supports filtering based on various criteria to narrow down the results."
    )
    public Page<VDepartmentMembershipWithDetailsEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VDepartmentMembershipWithDetailsFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.hasSystemPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ)) {
            if (filter.getDepartmentId() != null) {
                permissionService.requireDepartmentPermission(
                        user.getId(),
                        filter.getDepartmentId(),
                        DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ
                );
            } else {
                var accessibleDepartmentIds = permissionService
                        .getDepartmentsWithPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ);

                if (filter.getDepartmentIds() != null) {
                    accessibleDepartmentIds = filter.getDepartmentIds()
                            .stream()
                            .filter(accessibleDepartmentIds::contains)
                            .toList();
                }

                if (accessibleDepartmentIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                filter.setDepartmentIds(accessibleDepartmentIds);
            }
        }

        var page = service.list(pageable, filter);

        if (!permissionService.hasSystemPermission(user.getId(), DomainRolePermissionProvider.DOMAIN_ROLE_READ)) {
            return page.map(VDepartmentMembershipWithDetailsController::redactDomainRoleDetails);
        }

        return page;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Department Membership with Details by ID",
            description = "Retrieves detailed information about a specific department membership identified by its ID."
    )
    public VDepartmentMembershipWithDetailsEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var membership = service
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        permissionService.requireDepartmentPermission(
                user.getId(),
                membership.getDepartmentId(),
                DepartmentPermissionProvider.DEPARTMENT_MEMBERSHIP_READ
        );

        if (!permissionService.hasSystemPermission(user.getId(), DomainRolePermissionProvider.DOMAIN_ROLE_READ)) {
            return redactDomainRoleDetails(membership);
        }

        return membership;
    }

    private static VDepartmentMembershipWithDetailsEntity redactDomainRoleDetails(VDepartmentMembershipWithDetailsEntity membership) {
        // Membership read permission grants access to the membership itself, but not to domain role metadata.
        return membership
                .setDomainRoles(List.of())
                .setDomainRoleAssignments(List.of())
                .setDomainRolePermissions(List.of());
    }
}
