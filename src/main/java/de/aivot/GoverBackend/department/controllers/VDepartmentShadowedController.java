package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import de.aivot.GoverBackend.department.filters.VDepartmentShadowedFilter;
import de.aivot.GoverBackend.department.services.VDepartmentShadowedService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
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

@RestController
@RequestMapping("/api/departments-shadowed/")
@Tag(
        name = "Departments",
        description = "Departments are organisational units within the system. " +
                      "They can represent different sub-organizations, departments, or divisions within an organisation. " +
                      "Departments help in structuring users and managing permissions effectively. " +
                      "They also own certain resources and can have specific settings that apply to all users within the department."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class VDepartmentShadowedController {
    private final VDepartmentShadowedService vDepartmentShadowedService;

    @Autowired
    public VDepartmentShadowedController(VDepartmentShadowedService vDepartmentShadowedService) {
        this.vDepartmentShadowedService = vDepartmentShadowedService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Shadowed Departments",
            description = "Retrieve a paginated list of shadowed departments, including inherited or shadowed fields from parent departments."
    )
    public Page<VDepartmentShadowedEntity> list(
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid VDepartmentShadowedFilter filter
    ) throws ResponseException {
        return vDepartmentShadowedService
                .list(pageable, filter);
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Shadowed Department",
            description = "Retrieve a specific shadowed department by its ID, including inherited or shadowed fields from parent departments."
    )
    public VDepartmentShadowedEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return vDepartmentShadowedService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
