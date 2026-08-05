package de.aivot.gover.backend.search.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import de.aivot.gover.backend.search.entities.SearchItemEntity;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class SearchFilter implements Filter<SearchItemEntity> {
    @Nullable
    private final String userId;
    @Nonnull
    private final String search;
    @Nullable
    private final String originTable;
    @Nonnull
    private final String[] allowedProcessNodeDefinitionKeys;
    @Nonnull
    private final List<PermissionProvider.SearchPermission> searchPermissions;

    public SearchFilter(@Nullable String userId,
                        @Nonnull String search,
                        @Nullable String originTable,
                        @Nonnull String[] allowedProcessNodeDefinitionKeys,
                        @Nonnull List<PermissionProvider.SearchPermission> searchPermissions) {
        this.userId = userId;
        this.search = search;
        this.originTable = originTable;
        this.allowedProcessNodeDefinitionKeys = allowedProcessNodeDefinitionKeys;
        this.searchPermissions = searchPermissions;
    }

    @Override
    public Specification<SearchItemEntity> build() {
        if (StringUtils.isNullOrEmpty(userId) || searchPermissions.isEmpty()) {
            return SearchItemSpecifications.visibleToUser(
                    userId,
                    allowedProcessNodeDefinitionKeys,
                    searchPermissions
            );
        }

        return Specification.allOf(
                SearchItemSpecifications.visibleToUser(
                        userId,
                        allowedProcessNodeDefinitionKeys,
                        searchPermissions
                ),
                SearchItemSpecifications.matchesSearch(search, originTable)
        );
    }
}
