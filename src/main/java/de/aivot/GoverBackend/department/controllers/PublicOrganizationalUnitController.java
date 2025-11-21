package de.aivot.GoverBackend.department.controllers;

import de.aivot.GoverBackend.department.dtos.OrganizationalUnitResponseDTO;
import de.aivot.GoverBackend.department.services.VOrganizationalUnitShadowedService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nonnull;

/**
 * REST controller for accessing public organizational unit information for citizens.
 * <p>
 * Provides endpoints to retrieve organizational unit details using the shadowed organizational unit view.
 * This is intended for public/citizen-facing APIs and does not require authentication.
 * </p>
 */
@RestController
@RequestMapping("/api/public/organizational-units/")
public class PublicOrganizationalUnitController {
    /**
     * Service for retrieving shadowed organizational unit data.
     */
    private final VOrganizationalUnitShadowedService vOrganizationalUnitShadowedService;

    /**
     * Creates a new PublicOrganizationalUnitController with the required service.
     *
     * @param vOrganizationalUnitShadowedService the service used to access shadowed organizational units
     */
    @Autowired
    public PublicOrganizationalUnitController(VOrganizationalUnitShadowedService vOrganizationalUnitShadowedService) {
        this.vOrganizationalUnitShadowedService = vOrganizationalUnitShadowedService;
    }

    /**
     * Retrieves the organizational unit details for the specified ID.
     *
     * @param id the unique identifier of the organizational unit
     * @return the response DTO containing organizational unit details
     * @throws ResponseException if the organizational unit is not found or an error occurs during retrieval
     */
    @GetMapping("{id}/")
    public OrganizationalUnitResponseDTO retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return vOrganizationalUnitShadowedService
                .retrieve(id)
                .map(OrganizationalUnitResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }
}
