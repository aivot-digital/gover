package de.aivot.GoverBackend.utils.specification;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Collection;

public record SpecificationBuilderArrayContains<T>(
        @Nonnull String field,
        @Nonnull Object value
) implements SpecificationBuilderItem<T> {
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var searchFunc = builder.function(
                "sql",
                Boolean.class,
                builder.literal("? @> ARRAY[?]"),
                root.get(field),
                builder.literal(value)
        );

        return builder.isTrue(searchFunc);
    }
}
