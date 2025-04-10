package de.aivot.GoverBackend.utils.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import javax.annotation.Nonnull;
import java.util.Collection;

public record SpecificationBuilderInList<T>(
        @Nonnull String field,
        @Nonnull Collection<?> value
) implements SpecificationBuilderItem<T> {
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return root
                .get(field)
                .in(value);
    }
}
