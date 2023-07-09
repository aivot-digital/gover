package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.entities.Theme;
import de.aivot.GoverBackend.permissions.IsAdmin;
import de.aivot.GoverBackend.repositories.ApplicationRepository;
import de.aivot.GoverBackend.repositories.ThemeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class ThemeController {
    private final ThemeRepository repository;
    private final ApplicationRepository applicationRepository;

    @Autowired
    public ThemeController(ThemeRepository repository, ApplicationRepository applicationRepository) {
        this.repository = repository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/api/themes")
    public Collection<Theme> list() {
        return repository.findAll();
    }

    @IsAdmin
    @PostMapping("/api/themes")
    public Theme create(
            Authentication authentication,
            @RequestBody Theme newTheme
    ) {
        return repository.save(newTheme);
    }

    @GetMapping("/api/themes/{id}")
    public Theme retrieve(@PathVariable Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/api/public/themes/{id}")
    public Theme retrievePublic(@PathVariable Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @IsAdmin
    @PutMapping("/api/themes/{id}")
    public Theme update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody Theme updatedLink
    ) {
        Optional<Theme> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
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

    @IsAdmin
    @DeleteMapping("/api/themes/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        Optional<Theme> optLink = repository.findById(id);
        if (optLink.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (applicationRepository.existsByTheme_Id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        repository.delete(optLink.get());
    }
}
