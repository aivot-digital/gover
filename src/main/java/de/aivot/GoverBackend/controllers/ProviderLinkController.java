package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.entities.ProviderLink;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.repositories.ProviderLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class ProviderLinkController {
    private final ProviderLinkRepository repository;

    @Autowired
    public ProviderLinkController(ProviderLinkRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/provider-links")
    public Collection<ProviderLink> list() {
        return repository.findAll();
    }

    @IsAdmin
    @PostMapping("/api/provider-links")
    public ProviderLink create(
            Authentication authentication,
            @RequestBody ProviderLink newLink
    ) {
        return repository.save(newLink);
    }

    @GetMapping("/api/provider-links/{id}")
    public ProviderLink retrieve(@PathVariable Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @IsAdmin
    @PutMapping("/api/provider-links/{id}")
    public ProviderLink update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody ProviderLink updatedLink
    ) {
        Optional<ProviderLink> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        ProviderLink link = optLink.get();
        link.setLink(updatedLink.getLink());
        link.setText(updatedLink.getText());

        return repository.save(link);
    }

    @IsAdmin
    @DeleteMapping("/api/provider-links/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Optional<ProviderLink> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        repository.delete(optLink.get());
    }
}
