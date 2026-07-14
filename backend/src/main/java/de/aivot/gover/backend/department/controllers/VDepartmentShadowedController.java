package de.aivot.gover.backend.department.controllers;

import de.aivot.gover.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.gover.backend.department.filters.VDepartmentShadowedFilter;
import de.aivot.gover.backend.department.permissions.DepartmentPermissionProvider;
import de.aivot.gover.backend.department.services.VDepartmentShadowedService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.openApi.OpenApiConstants;
import de.aivot.gover.backend.permissions.services.PermissionService;
import de.aivot.gover.backend.user.services.UserService;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/departments-shadowed/")
@Tag(
        name = OpenApiConstants.Tags.DepartmentsName,
        description = OpenApiConstants.Tags.DepartmentsDescription
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VDepartmentShadowedController {
    private final VDepartmentShadowedService vDepartmentShadowedService;
    private final PermissionService permissionService;
    private final UserService userService;

    @Autowired
    public VDepartmentShadowedController(VDepartmentShadowedService vDepartmentShadowedService,
                                         PermissionService permissionService,
                                         UserService userService) {
        this.vDepartmentShadowedService = vDepartmentShadowedService;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Shadowed Departments",
            description = "Retrieve a paginated list of shadowed departments, including inherited or shadowed fields from parent departments."
    )
    public Page<VDepartmentShadowedEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VDepartmentShadowedFilter filter
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!permissionService.checkSystemPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_READ)) {
            List<Integer> readableDepartmentIds;

            if (filter.getId() != null) {
                permissionService.hasDepartmentPermission(
                        user.getId(),
                        filter.getId(),
                        DepartmentPermissionProvider.DEPARTMENT_READ
                );
                readableDepartmentIds = List.of(filter.getId());
            } else {
                readableDepartmentIds = permissionService
                        .getDepartmentsWithPermission(user.getId(), DepartmentPermissionProvider.DEPARTMENT_READ);

                if (filter.getIds() != null) {
                    // Keep explicit client filters intact while preventing inaccessible departments from leaking through the shadowed view.
                    readableDepartmentIds = filter.getIds()
                            .stream()
                            .filter(readableDepartmentIds::contains)
                            .toList();
                }

                if (readableDepartmentIds.isEmpty()) {
                    return Page.empty(pageable);
                }

                if (Boolean.TRUE.equals(filter.getIncludeAncestors())) {
                    filter.setIds(expandDepartmentIdsWithAncestorIds(readableDepartmentIds));
                } else {
                    filter.setIds(readableDepartmentIds);
                }
            }

            var result = vDepartmentShadowedService
                    .list(pageable, filter);

            if (!Boolean.TRUE.equals(filter.getIncludeAncestors())) {
                return result;
            }

            var readableDepartmentIdSet = Set.copyOf(readableDepartmentIds);
            return result.map(department -> readableDepartmentIdSet.contains(department.getId()) ?
                    department :
                    createHierarchyContextDepartment(department));
        }

        return vDepartmentShadowedService
                .list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Shadowed Department",
            description = "Retrieve a specific shadowed department by its ID, including inherited or shadowed fields from parent departments."
    )
    public VDepartmentShadowedEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        permissionService.hasDepartmentPermission(
                user.getId(),
                id,
                DepartmentPermissionProvider.DEPARTMENT_READ
        );

        return vDepartmentShadowedService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    private List<Integer> expandDepartmentIdsWithAncestorIds(List<Integer> departmentIds) throws ResponseException {
        var departmentIdSet = new LinkedHashSet<Integer>();
        var departments = vDepartmentShadowedService
                .list(Pageable.unpaged(), VDepartmentShadowedFilter.create().setIds(departmentIds))
                .getContent();

        for (var department : departments) {
            var parentIds = department.getParentIds();
            if (parentIds != null) {
                departmentIdSet.addAll(parentIds);
            }
            departmentIdSet.add(department.getId());
        }

        return departmentIdSet
                .stream()
                .toList();
    }

    private VDepartmentShadowedEntity createHierarchyContextDepartment(VDepartmentShadowedEntity department) {
        // Ancestors without direct read permission are only returned as structural context for tree views.
        return new VDepartmentShadowedEntity()
                .setId(department.getId())
                .setName(department.getName())
                .setPostalAddress(department.getPostalAddress())
                .setCreated(department.getCreated())
                .setUpdated(department.getUpdated())
                .setDepth(department.getDepth())
                .setParentDepartmentId(department.getParentDepartmentId())
                .setParentNames(department.getParentNames())
                .setParentIds(department.getParentIds());
    }
}
