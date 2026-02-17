package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;

@RestController
@RequestMapping("/api/department-memberships-with-permissions/")
@Tag(
        name = "Department Memberships",
        description = "Department Memberships link users to organisational units (departments) within the system. " +
                      "They define which users belong to which departments and what roles or permissions they have within those departments. " +
                      "Managing department memberships is crucial for controlling access to resources and functionalities based on organisational structure."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class VDepartmentMembershipWithPermissionsController {
    private final VDepartmentMembershipWithPermissionsService service;

    @Autowired
    public VDepartmentMembershipWithPermissionsController(VDepartmentMembershipWithPermissionsService service) {
        this.service = service;
    }

    @GetMapping("")
    @Operation(
            summary = "List Department Memberships with Permissions",
            description = "Retrieves a paginated list of department memberships along with permission information for each membership. " +
                          "Supports filtering based on various criteria to narrow down the results."
    )
    public Page<VDepartmentMembershipWithPermissionsEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VDepartmentMembershipWithPermissionsFilter filter
    ) throws ResponseException {
        return service.list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Department Membership with Permissions by ID",
            description = "Retrieves permission information about a specific department membership identified by its ID."
    )
    public VDepartmentMembershipWithPermissionsEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return service
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}

