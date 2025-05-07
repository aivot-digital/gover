package de.aivot.GoverBackend.utils.specification;

import jakarta.persistence.criteria.*;

import javax.annotation.Nonnull;
import java.util.List;

public record SpecificationBuilderJsonArrayElementFieldEquals<T>(
        @Nonnull String field,
        @Nonnull String elementField,
        @Nonnull String value
) implements SpecificationBuilderItem<T> {
    private final static String JsonArrayElementHasValueFunctionName = "jsonb_array_element_has_value";

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var args = new Expression[3];

        args[0] = root.get(field);
        args[1] = builder.literal(elementField);
        args[2] = builder.literal(value);

        var extractExpression = builder
                .function(JsonArrayElementHasValueFunctionName, Object.class, args);

        return builder
                .equal(extractExpression, true);
    }
}
