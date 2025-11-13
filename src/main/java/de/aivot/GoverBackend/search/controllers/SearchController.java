package de.aivot.GoverBackend.search.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.search.entities.SearchItemEntity;
import de.aivot.GoverBackend.search.repositories.SearchEntityRepository;
import de.aivot.GoverBackend.user.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This controller is responsible for handling requests to the secrets API.
 * A secret is used to store sensitive information like passwords, API keys, etc.
 */
@RestController
@RequestMapping("/api/search/")
public class SearchController {

    private final SearchEntityRepository searchEntityRepository;

    @Autowired
    public SearchController(SearchEntityRepository searchEntityRepository) {
        this.searchEntityRepository = searchEntityRepository;
    }

    @GetMapping("")
    public Page<SearchItemEntity> search(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @RequestParam(defaultValue = "") String search
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return searchEntityRepository
                .search(search, pageable);
    }
}
