package de.aivot.gover.backend.search.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.permissions.models.PermissionProvider;
import de.aivot.gover.backend.search.entities.SearchItemEntity;
import de.aivot.gover.backend.utils.StringUtils;
import de.aivot.gover.backend.utils.specification.SpecificationBuilderArrayContains;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SearchFilter implements Filter<SearchItemEntity> {
    private static final double MIN_SIMILARITY = 0.1;

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
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.isNullOrEmpty(userId) || searchPermissions.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            var predicates = new LinkedList<Predicate>();
            var similarity = criteriaBuilder.function(
                    "word_similarity",
                    Double.class,
                    root.get("searchText"),
                    criteriaBuilder.literal(search)
            );

            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            predicates.add(criteriaBuilder.greaterThan(similarity, MIN_SIMILARITY));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.notEqual(root.get("originTable"), "process_nodes"),
                    root.get("originTableSubset").in(Arrays.asList(allowedProcessNodeDefinitionKeys))
            ));

            if (StringUtils.isNotNullOrEmpty(originTable)) {
                predicates.add(criteriaBuilder.equal(root.get("originTable"), originTable));
            }

            var permissionPredicates = searchPermissions
                    .stream()
                    .map(searchPermission -> criteriaBuilder.and(
                            // Keep origin_table and permission paired because each search row carries
                            // a user's whole permission array, not just the permission for this origin.
                            criteriaBuilder.equal(root.get("originTable"), searchPermission.originTable()),
                            criteriaBuilder.isTrue(SpecificationBuilderArrayContains.getFunc(
                                    criteriaBuilder,
                                    root,
                                    "permissions",
                                    searchPermission.searchPermission()
                            ))
                    ))
                    .toArray(Predicate[]::new);

            predicates.add(criteriaBuilder.or(permissionPredicates));

            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                query.orderBy(criteriaBuilder.desc(similarity));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
