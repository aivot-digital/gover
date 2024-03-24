package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.ProviderLink;
import de.aivot.GoverBackend.repositories.ProviderLinkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;

@RestController
public class ProviderLinkController {
    private final ProviderLinkRepository linkRepository;

    @Autowired
    public ProviderLinkController(ProviderLinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    @GetMapping("/api/provider-links")
    public Collection<ProviderLink> list() {
        return linkRepository.findAll();
    }

    @PostMapping("/api/provider-links")
    public ProviderLink create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ProviderLink newLink
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        return linkRepository.save(newLink);
    }

    @GetMapping("/api/provider-links/{id}")
    public ProviderLink retrieve(
            @PathVariable Integer id
    ) {
        return linkRepository
                .findById(id)
                .orElseThrow(ResourceNotFoundException::new);
    }

    @PutMapping("/api/provider-links/{id}")
    public ProviderLink update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody ProviderLink updatedLink
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        var link = linkRepository
                .findById(id)
                .orElseThrow(ResourceNotFoundException::new);

        link.setLink(updatedLink.getLink());
        link.setText(updatedLink.getText());

        return linkRepository.save(link);
    }

    @DeleteMapping("/api/provider-links/{id}")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        var link = linkRepository
                .findById(id)
                .orElseThrow(ResourceNotFoundException::new);

        linkRepository.delete(link);
    }
}
