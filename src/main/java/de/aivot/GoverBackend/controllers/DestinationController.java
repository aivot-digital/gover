package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.entities.Destination;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class DestinationController {
    private final DestinationRepository repository;

    @Autowired
    public DestinationController(DestinationRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/destinations")
    public Collection<Destination> list() {
        return repository.findAll();
    }

    @IsAdmin
    @PostMapping("/api/destinations")
    public Destination create(
            Authentication authentication,
            @RequestBody Destination newDest
    ) {
        return repository.save(newDest);
    }

    @GetMapping("/api/destinations/{id}")
    public Destination retrieve(@PathVariable Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @IsAdmin
    @PutMapping("/api/destinations/{id}")
    public Destination update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody Destination updatedDes
    ) {
        Optional<Destination> optDest = repository.findById(id);
        if (optDest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Destination dest = optDest.get();
        dest.setName(updatedDes.getName());
        dest.setType(updatedDes.getType());

        dest.setMailTo(updatedDes.getMailTo());
        dest.setMailCC(updatedDes.getMailCC());
        dest.setMailBCC(updatedDes.getMailBCC());

        dest.setApiAddress(updatedDes.getApiAddress());
        dest.setAuthorizationHeader(updatedDes.getAuthorizationHeader());

        dest.setMaxAttachmentMegaBytes(updatedDes.getMaxAttachmentMegaBytes());

        return repository.save(dest);
    }

    @IsAdmin
    @DeleteMapping("/api/destinations/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Optional<Destination> optDest = repository.findById(id);
        if (optDest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        repository.delete(optDest.get());
    }
}
