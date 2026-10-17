package de.aivot.prosuna.backend.lib.services;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SpecificationCountService {
    private final EntityManager entityManager;

    public SpecificationCountService(@Nonnull EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /** Counts overlapping categories in one query; callers supply the same access scope as their list. */
    @Nonnull
    public <T> Map<String, Long> count(@Nonnull Class<T> entityClass,
                                     @Nullable Specification<T> scope,
                                     @Nonnull Map<String, Specification<T>> categories) {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createTupleQuery();
        var root = query.from(entityClass);
        var selections = new ArrayList<Selection<?>>();
        for (var entry : categories.entrySet()) {
            var predicate = entry.getValue().toPredicate(root, query, builder);
            selections.add(builder.count(builder.<Integer>selectCase()
                    .when(predicate == null ? builder.conjunction() : predicate, 1)
                    .otherwise((Integer) null)).alias(entry.getKey()));
        }
        query.multiselect(selections);
        if (scope != null) {
            var predicate = scope.toPredicate(root, query, builder);
            if (predicate != null) query.where(predicate);
        }
        var row = entityManager.createQuery(query).getSingleResult();
        var result = new LinkedHashMap<String, Long>();
        categories.keySet().forEach(key -> result.put(key, row.get(key, Long.class)));
        return result;
    }
}
