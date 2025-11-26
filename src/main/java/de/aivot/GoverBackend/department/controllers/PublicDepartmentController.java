package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import de.aivot.GoverBackend.department.services.VDepartmentShadowedService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for accessing public organizational unit information for citizens.
 * <p>
 * Provides endpoints to retrieve organizational unit details using the shadowed organizational unit view.
 * This is intended for public/citizen-facing APIs and does not require authentication.
 * </p>
 */
@RestController
@RequestMapping("/api/public/departments/")
public class PublicDepartmentController {
    /**
     * Service for retrieving shadowed organizational unit data.
     */
    private final VDepartmentShadowedService vDepartmentShadowedService;

    /**
     * Creates a new PublicOrganizationalUnitController with the required service.
     *
     * @param vDepartmentShadowedService the service used to access shadowed organizational units
     */
    @Autowired
    public PublicDepartmentController(VDepartmentShadowedService vDepartmentShadowedService) {
        this.vDepartmentShadowedService = vDepartmentShadowedService;
    }

    /**
     * Retrieves the organizational unit details for the specified ID.
     *
     * @param id the unique identifier of the organizational unit
     * @return the response DTO containing organizational unit details
     * @throws ResponseException if the organizational unit is not found or an error occurs during retrieval
     */
    @GetMapping("{id}/")
    public VDepartmentShadowedEntity retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return vDepartmentShadowedService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
