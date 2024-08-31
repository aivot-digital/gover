package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.exceptions.ConflictException;
import de.aivot.GoverBackend.exceptions.ForbiddenException;
import de.aivot.GoverBackend.exceptions.NotFoundException;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.entities.Theme;
import de.aivot.GoverBackend.repositories.FormRepository;
import de.aivot.GoverBackend.repositories.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collection;
import java.util.Optional;

@RestController
public class ThemeController {
    private final ThemeRepository repository;
    private final FormRepository formRepository;

    @Autowired
    public ThemeController(ThemeRepository repository, FormRepository formRepository) {
        this.repository = repository;
        this.formRepository = formRepository;
    }

    @GetMapping("/api/themes")
    public Collection<Theme> list() {
        return repository.findAll();
    }

    @PostMapping("/api/themes")
    public Theme create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Theme newTheme
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        return repository.save(newTheme);
    }

    @GetMapping("/api/themes/{id}")
    public Theme retrieve(
            @PathVariable Integer id
    ) {
        return repository
                .findById(id)
                .orElseThrow(NotFoundException::new);
    }

    @GetMapping("/api/public/themes/{id}")
    public Theme retrievePublic(@PathVariable Integer id) {
        return repository
                .findById(id)
                .orElseThrow(NotFoundException::new);
    }

    @PutMapping("/api/themes/{id}")
    public Theme update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody Theme updatedLink
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        Optional<Theme> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new NotFoundException();
        }

        Theme link = optLink.get();
        link.setName(updatedLink.getName());
        link.setMain(updatedLink.getMain());
        link.setMainDark(updatedLink.getMainDark());
        link.setAccent(updatedLink.getAccent());
        link.setError(updatedLink.getError());
        link.setWarning(updatedLink.getWarning());
        link.setInfo(updatedLink.getInfo());
        link.setSuccess(updatedLink.getSuccess());

        return repository.save(link);
    }

    @DeleteMapping("/api/themes/{id}")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        KeyCloakDetailsUser
                .fromJwt(jwt)
                .ifNotAdminThrow(ForbiddenException::new);

        Optional<Theme> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new NotFoundException();
        }

        if (formRepository.existsByThemeId(id)) {
            throw new ConflictException();
        }

        repository.delete(optLink.get());
    }
}
