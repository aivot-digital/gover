package de.aivot.gover.backend.search.controllers;

import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.openApi.OpenApiConfiguration;
import de.aivot.gover.backend.plugin.services.PluginUtils;
import de.aivot.gover.backend.plugins.form.FormPlugin;
import de.aivot.gover.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.gover.backend.search.entities.SearchItemEntity;
import de.aivot.gover.backend.search.repositories.SearchEntityRepository;
import de.aivot.gover.backend.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller is responsible for handling requests to the secrets API. A secret is used to store sensitive information like passwords, API keys, etc.
 */
@RestController
@RequestMapping("/api/search/")
@Tag(
        name = "Search",
        description = "Endpoints for searching various entities within the application."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class SearchController {
    private final SearchEntityRepository searchEntityRepository;

    private static final String[] allowedProcessNodeDefinitionKeys = {
            PluginUtils.combineComponentKey(FormPlugin.PLUGIN_KEY, FormTriggerNodeV1.NODE_KEY),
    };

    public SearchController(SearchEntityRepository searchEntityRepository) {
        this.searchEntityRepository = searchEntityRepository;
    }

    @GetMapping("")
    @Operation(
            summary = "Search Entities",
            description = "Search various entities within the application."
    )
    public Page<SearchItemEntity> search(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @RequestParam(defaultValue = "") String search,
            @Nullable @RequestParam(required = false) String originTable
    ) throws ResponseException {
        if (StringUtils.isNotNullOrEmpty(originTable)) {
            return searchEntityRepository
                    .search(search, originTable, allowedProcessNodeDefinitionKeys, pageable);
        }

        return searchEntityRepository
                .search(search, allowedProcessNodeDefinitionKeys, pageable);
    }
}
