package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.FormSlugHistoryEntity;
import de.aivot.GoverBackend.form.filters.FormSlugHistoryFilter;
import de.aivot.GoverBackend.form.repositories.FormRepository;
import de.aivot.GoverBackend.form.repositories.FormSlugHistoryRepository;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@RestController
public class FormSlugHistoryController {
    private final FormSlugHistoryRepository formSlugHistoryRepository;
    private final FormRepository formRepository;

    @Autowired
    public FormSlugHistoryController(FormSlugHistoryRepository formSlugHistoryRepository, FormRepository formRepository) {
        this.formSlugHistoryRepository = formSlugHistoryRepository;
        this.formRepository = formRepository;
    }

    @GetMapping("/api/form-slugs/")
    public Page<FormSlugHistoryEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormSlugHistoryFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return formSlugHistoryRepository
                .findAll(filter.build(), pageable);
    }

    @GetMapping("/api/form-slugs/{slug}/")
    public boolean test(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable String slug
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return
                formRepository
                        .existsBySlug(slug) ||
                formSlugHistoryRepository
                        .existsById(slug);
    }
}
