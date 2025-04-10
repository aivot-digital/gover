package de.aivot.GoverBackend.utils.specification;

import jakarta.persistence.criteria.*;

import javax.annotation.Nonnull;
import java.util.List;

public record SpecificationBuilderJsonEquals<T>(
        @Nonnull String field,
        @Nonnull List<String> path,
        @Nonnull String value
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
                .equal(extractExpression, value);
    }
}
