package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import de.aivot.GoverBackend.repositories.FormRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;

@RestController
public class DestinationController {
    private final DestinationRepository destinationRepository;
    private final FormRepository formRepository;

    @Autowired
    public DestinationController(
            DestinationRepository destinationRepository,
            FormRepository formRepository
    ) {
        this.destinationRepository = destinationRepository;
        this.formRepository = formRepository;
    }

    /**
     * List all destinations.
     *
     * @return A list of destinations.
     */
    @GetMapping("/api/destinations")
    public Collection<Destination> list() {
        return destinationRepository.findAll();
    }

    /**
     * Create a new destination.
     *
     * @param jwt     The JWT of the user.
     * @param newDest The destination to create.
     * @return The created destination.
     */
    @PostMapping("/api/destinations")
    public Destination create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Destination newDest
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        return destinationRepository.save(newDest);
    }

    /**
     * Retrieve a destination.
     *
     * @param id The id of the destination to retrieve.
     * @return The destination.
     */
    @GetMapping("/api/destinations/{id}")
    public Destination retrieve(
            @PathVariable Integer id
    ) {
        return destinationRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);
    }

    /**
     * Update a destination.
     *
     * @param jwt        The JWT of the user.
     * @param id         The id of the destination to update.
     * @param updatedDes The updated destination.
     * @return The updated destination.
     */
    @PutMapping("/api/destinations/{id}")
    public Destination update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody Destination updatedDes
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        var dest = destinationRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        dest.setName(updatedDes.getName());
        dest.setType(updatedDes.getType());

        dest.setMailTo(updatedDes.getMailTo());
        dest.setMailCC(updatedDes.getMailCC());
        dest.setMailBCC(updatedDes.getMailBCC());

        dest.setApiAddress(updatedDes.getApiAddress());
        dest.setAuthorizationHeader(updatedDes.getAuthorizationHeader());

        dest.setMaxAttachmentMegaBytes(updatedDes.getMaxAttachmentMegaBytes());

        return destinationRepository.save(dest);
    }

    /**
     * Delete a destination.
     *
     * @param jwt The JWT of the user.
     * @param id  The id of the destination to delete.
     */
    @DeleteMapping("/api/destinations/{id}")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        var dest = destinationRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        if (formRepository.existsByDestinationId(id)) {
            throw new ConflictException();
        }

        destinationRepository.delete(dest);
    }
}
