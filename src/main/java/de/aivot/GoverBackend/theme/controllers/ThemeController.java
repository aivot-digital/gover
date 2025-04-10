package de.aivot.GoverBackend.theme.controllers;

import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.theme.entities.Theme;
import de.aivot.GoverBackend.theme.filters.ThemeFilter;
import de.aivot.GoverBackend.theme.services.ThemeService;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
@RequestMapping("/api/themes/")
public class ThemeController {
    private final ThemeService service;
    private final FormRepository formRepository;

    @Autowired
    public ThemeController(
            ThemeService service,
            FormRepository formRepository
    ) {
        this.service = service;
        this.formRepository = formRepository;
    }

    @GetMapping("")
    public Page<Theme> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid ThemeFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .list(pageable, filter);
    }

    @PostMapping("")
    public Theme create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody Theme newTheme
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        return service.create(newTheme);
    }

    @GetMapping("{id}/")
    public Theme retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return service
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }


    @PutMapping("{id}/")
    public Theme update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id,
            @Nonnull @Valid @RequestBody Theme updatedTheme
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        return service
                .update(id, updatedTheme);
    }

    @DeleteMapping("{id}/")
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized)
                .asAdmin()
                .orElseThrow(ResponseException::forbidden);

        if (formRepository.existsByThemeId(id)) {
            throw new ResponseException(HttpStatus.CONFLICT, "Das Farbschema wird noch von einem oder mehreren Formularen verwendet.");
        }

        service.delete(id);
    }
}
