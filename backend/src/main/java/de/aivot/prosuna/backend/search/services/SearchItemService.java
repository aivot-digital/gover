package de.aivot.prosuna.backend.search.services;

import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.plugin.services.PluginUtils;
import de.aivot.prosuna.backend.plugins.form.FormPlugin;
import de.aivot.prosuna.backend.plugins.form.v1.nodes.FormTriggerNodeV1;
import de.aivot.prosuna.backend.search.dtos.SearchItemResponseDTO;
import de.aivot.prosuna.backend.search.entities.SearchItemEntity;
import de.aivot.prosuna.backend.search.filters.SearchFilter;
import de.aivot.prosuna.backend.search.filters.SearchItemSpecifications;
import de.aivot.prosuna.backend.search.repositories.SearchEntityRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SearchItemService {
    private final SearchEntityRepository searchEntityRepository;
    private final List<PermissionProvider.SearchPermission> searchPermissions;

    private static final String[] allowedProcessNodeDefinitionKeys = {
            PluginUtils.combineComponentKey(FormPlugin.PLUGIN_KEY, FormTriggerNodeV1.NODE_KEY),
    };

    public SearchItemService(SearchEntityRepository searchEntityRepository,
                             List<PermissionProvider> permissionProviders) {
        this.searchEntityRepository = searchEntityRepository;

        searchPermissions = permissionProviders
                .stream()
                .flatMap(permissionProvider -> permissionProvider.getSearchPermissions().stream())
                .toList();
    }

    @Nonnull
    public Page<SearchItemResponseDTO> search(
            @Nullable String userId,
            @Nonnull Pageable pageable,
            @Nonnull String search,
            @Nullable String originTable
    ) {
        var spec = new SearchFilter(
                userId,
                search,
                originTable,
                allowedProcessNodeDefinitionKeys,
                searchPermissions
        )
                .build();

        return searchEntityRepository
                .findAll(spec, pageable)
                .map(SearchItemResponseDTO::fromEntity);
    }

    @Nonnull
    public Optional<SearchItemResponseDTO> retrieveVisible(
            @Nullable String userId,
            @Nonnull String originTable,
            @Nonnull String id
    ) {
        Specification<SearchItemEntity> spec = Specification.allOf(
                SearchItemSpecifications.visibleToUser(
                        userId,
                        allowedProcessNodeDefinitionKeys,
                        searchPermissions
                ),
                SearchItemSpecifications.hasIdentity(originTable, id)
        );

        return searchEntityRepository
                .findAll(spec, PageRequest.of(0, 1))
                .getContent()
                .stream()
                .findFirst()
                .map(SearchItemResponseDTO::fromEntity);
    }
}
