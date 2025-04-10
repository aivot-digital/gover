package de.aivot.GoverBackend.utils.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import javax.annotation.Nonnull;

public record SpecificationBuilderNotEquals<T>(
        @Nonnull String field,
        @Nonnull Object value
) implements SpecificationBuilderItem<T> {
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        return builder.notEqual(root.get(field), value);
    }
}
