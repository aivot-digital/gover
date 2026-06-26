package de.aivot.gover.backend.department.controllers;

import de.aivot.gover.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.gover.backend.department.services.VDepartmentShadowedService;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConstants;
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
        name = OpenApiConstants.Tags.DepartmentsName,
        description = OpenApiConstants.Tags.DepartmentsDescription
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
