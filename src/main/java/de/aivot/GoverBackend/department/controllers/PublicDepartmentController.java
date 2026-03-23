package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import de.aivot.GoverBackend.department.services.VDepartmentShadowedService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/departments/")
@Tag(
        name = "Departments",
        description = "Departments are organisational units within the system. " +
                      "They can represent different sub-organizations, departments, or divisions within an organisation. " +
                      "Departments help in structuring users and managing permissions effectively. " +
                      "They also own certain resources and can have specific settings that apply to all users within the department."
)
public class PublicDepartmentController {

    private final VDepartmentShadowedService vDepartmentShadowedService;

    @Autowired
    public PublicDepartmentController(VDepartmentShadowedService vDepartmentShadowedService) {
        this.vDepartmentShadowedService = vDepartmentShadowedService;
    }

    @GetMapping("{id}/")
    @Operation(
            summary = "Retrieve Public Department by ID",
            description = "Retrieves a public view of the department specified by its ID. " +
                          "This endpoint provides limited information suitable for public access."
    )
    public VDepartmentShadowedEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return vDepartmentShadowedService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
