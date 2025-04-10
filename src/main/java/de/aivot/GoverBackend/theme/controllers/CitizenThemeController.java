package de.aivot.GoverBackend.theme.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.theme.entities.Theme;
import de.aivot.GoverBackend.theme.services.ThemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;

@RestController
@RequestMapping("/api/public/themes/")
public class CitizenThemeController {
    private final ThemeService service;

    @Autowired
    public CitizenThemeController(
            ThemeService service
    ) {
        this.service = service;
    }

    @GetMapping("{id}/")
    public Theme retrieve(
            @Nonnull @PathVariable Integer id
    ) throws ResponseException {
        return service
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }
}
