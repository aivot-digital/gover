package de.aivot.GoverBackend.utils.specification;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.*;

import java.util.List;

public record SpecificationBuilderJsonEqualsNull<T>(
        @Nonnull String field,
        @Nonnull List<String> path
) implements SpecificationBuilderItem<T> {
    private final static String JsonExtractPathFunctionName = "jsonb_extract_path_text";

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var args = new Expression[path.size() + 1];

        args[0] = root.get(field);

        for (int i = 0; i < path.size(); i++) {
            args[i+1] = builder.literal(path.get(i)).as(String.class);
        }

        var extractExpression = builder
                .function(JsonExtractPathFunctionName, Object.class, args);

        return builder
                .isNull(extractExpression);
    }
}
