package de.aivot.GoverBackend.search.controllers;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.search.entities.SearchItemEntity;
import de.aivot.GoverBackend.search.repositories.SearchEntityRepository;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * This controller is responsible for handling requests to the secrets API.
 * A secret is used to store sensitive information like passwords, API keys, etc.
 */
@RestController
@RequestMapping("/api/search/")
@Tag(
        name = "Search",
        description = "Endpoints for searching various entities within the application."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SearchController {

    //private final SearchEntityRepository searchEntityRepository;

    public SearchController(/*SearchEntityRepository searchEntityRepository*/) {
        //this.searchEntityRepository = searchEntityRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "Search Entities",
            description = "Search various entities within the application."
    )
    public Page<SearchItemEntity> search(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @RequestParam(defaultValue = "") String search
    ) throws ResponseException {
        return Page.empty();
        /*return searchEntityRepository
                .search(search, pageable);*/
    }
}
