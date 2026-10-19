package de.aivot.prosuna.backend.search.filters;

import de.aivot.prosuna.backend.permissions.models.PermissionProvider;
import de.aivot.prosuna.backend.process.utils.CrockfordCaseNumber;
import de.aivot.prosuna.backend.search.entities.SearchItemEntity;
import de.aivot.prosuna.backend.utils.StringUtils;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilderArrayContains;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public final class SearchItemSpecifications {
    private static final double MIN_SIMILARITY = 0.1;

    private SearchItemSpecifications() {
    }

    @Nonnull
    public static Specification<SearchItemEntity> visibleToUser(
            @Nullable String userId,
            @Nonnull String[] allowedProcessNodeDefinitionKeys,
            @Nonnull List<PermissionProvider.SearchPermission> searchPermissions
    ) {
        return (root, query, criteriaBuilder) -> {
            if (StringUtils.isNullOrEmpty(userId) || searchPermissions.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            var predicates = new LinkedList<Predicate>();
            predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.notEqual(root.get("originTable"), "process_nodes"),
                    root.get("originTableSubset").in(Arrays.asList(allowedProcessNodeDefinitionKeys))
            ));

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

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Nonnull
    public static Specification<SearchItemEntity> matchesSearch(
            @Nonnull String search,
            @Nullable String originTable
    ) {
        return (root, query, criteriaBuilder) -> {
            var searchTerm = search.trim();
            if (searchTerm.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            var predicates = new LinkedList<Predicate>();
            // word_similarity finds the first argument within the second. Keep the search term first:
            // additional file numbers or search aliases must not dilute an otherwise matching term.
            var similarity = criteriaBuilder.function(
                    "word_similarity",
                    Double.class,
                    criteriaBuilder.literal(searchTerm),
                    root.get("searchText")
            );

            var searchText = criteriaBuilder.lower(root.<String>get("searchText"));
            var literalMatch = criteriaBuilder.like(searchText,
                    "%" + escapeLike(searchTerm.toLowerCase(Locale.ROOT)) + "%", '\\');
            var normalized = CrockfordCaseNumber.normalizeSearch(searchTerm);
            // Only instance search texts include Crockford aliases. Keep the original query as well,
            // since replacing O/I/L in ordinary names, UUIDs or custom case numbers changes their meaning.
            var compactMatch = normalized == null ? criteriaBuilder.disjunction()
                    : criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("originTable"), "process_instances"),
                            criteriaBuilder.like(searchText, "%" + normalized.toLowerCase(Locale.ROOT) + "%"));
            var directMatch = criteriaBuilder.or(literalMatch, compactMatch);
            predicates.add(criteriaBuilder.or(directMatch,
                    criteriaBuilder.greaterThan(similarity, MIN_SIMILARITY)));

            if (StringUtils.isNotNullOrEmpty(originTable)) {
                predicates.add(criteriaBuilder.equal(root.get("originTable"), originTable));
            }

            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                query.orderBy(
                        criteriaBuilder.asc(criteriaBuilder.<Integer>selectCase()
                                .when(directMatch, 0).otherwise(1)),
                        criteriaBuilder.desc(similarity),
                        criteriaBuilder.asc(root.get("originTable")), criteriaBuilder.asc(root.get("id")));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Nonnull
    private static String escapeLike(@Nonnull String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Nonnull
    public static Specification<SearchItemEntity> hasIdentity(
            @Nonnull String originTable,
            @Nonnull String id
    ) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.and(
                criteriaBuilder.equal(root.get("originTable"), originTable),
                criteriaBuilder.equal(root.get("id"), id)
        );
    }
}
