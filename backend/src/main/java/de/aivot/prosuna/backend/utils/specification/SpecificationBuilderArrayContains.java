package de.aivot.prosuna.backend.utils.specification;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.*;

public record SpecificationBuilderArrayContains<T>(
        @Nonnull String field,
        @Nonnull Object value
) implements SpecificationBuilderItem<T> {
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var searchFunc = getFunc(builder, root, field, value);

        return builder.isTrue(searchFunc);
    }

    public static <T> Expression<Boolean> getFunc(CriteriaBuilder builder, Root<T> root, String field, Object value) {
        return builder
                .function(
                        "sql",
                        Boolean.class,
                        builder.literal("CAST(? as text[]) @> ARRAY[?]::text[]"),
                        root.get(field),
                        builder.literal(value)
                );
    }
}
