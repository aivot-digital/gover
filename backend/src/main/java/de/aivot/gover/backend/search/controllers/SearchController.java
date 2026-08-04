package de.aivot.gover.backend.search.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.search.dtos.SearchItemResponseDTO;
import de.aivot.gover.backend.search.dtos.SearchRecentItemRequestDTO;
import de.aivot.gover.backend.search.services.SearchItemService;
import de.aivot.gover.backend.search.services.SearchRecentItemService;
import de.aivot.gover.backend.user.services.UserService;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search/")
@Tag(
        name = "Search",
        description = "Endpoints for searching various entities within the application."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SearchController {
    private final SearchItemService searchItemService;
    private final SearchRecentItemService searchRecentItemService;

    public SearchController(SearchItemService searchItemService,
                            SearchRecentItemService searchRecentItemService) {
        this.searchItemService = searchItemService;
        this.searchRecentItemService = searchRecentItemService;
    }

    @GetMapping("")
    @Operation(
            summary = "Search Entities",
            description = "Search various entities within the application."
    )
    public Page<SearchItemResponseDTO> search(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @RequestParam(defaultValue = "") String search,
            @Nullable @RequestParam(required = false) String originTable
    ) throws ResponseException {
        return searchItemService.search(
                UserService.getIdFromJWT(jwt),
                pageable,
                search,
                originTable
        );
    }

    @GetMapping("recent/")
    @Operation(
            summary = "List Recent Search Items",
            description = "List recently opened search items for the authenticated user. The result is filtered by current permissions."
    )
    public List<SearchItemResponseDTO> listRecentItems(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @RequestParam(defaultValue = "10") Integer size
    ) throws ResponseException {
        var userId = UserService.getIdFromJWT(jwt);
        if (StringUtils.isNullOrEmpty(userId)) {
            throw ResponseException.unauthorized();
        }

        return searchRecentItemService.listVisibleRecentItems(userId, size);
    }

    @PutMapping("recent/")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Record Recent Search Item",
            description = "Record a recently opened search item for the authenticated user. Items without current access are ignored."
    )
    public void recordRecentItem(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody SearchRecentItemRequestDTO request
    ) throws ResponseException {
        var userId = UserService.getIdFromJWT(jwt);
        if (StringUtils.isNullOrEmpty(userId)) {
            throw ResponseException.unauthorized();
        }

        searchRecentItemService.recordRecentItem(userId, request);
    }
}
