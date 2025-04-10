package de.aivot.GoverBackend.utils.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.stream.Collectors;

public record SpecificationBuilderContains<T>(
        @Nonnull String field,
        @Nonnull String value
) implements SpecificationBuilderItem<T> {
    private final static String ToTsVectorFunctionName = "to_tsvector";
    private final static String ToTsQueryFunctionName = "to_tsquery";

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var searchFunc = builder.function(
                "sql",
                Boolean.class,
                builder.literal("? ILIKE ?"),
                root.get(field),
                builder.literal("%" + value + "%")
        );

        return builder.isTrue(searchFunc);
    }

    /*
    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        var toTsVectorFunc = builder.function(
                ToTsVectorFunctionName,
                Object.class,
                root.get(field)
        );

        var searchPattern = Arrays
                .stream(value.split(" "))
                .map(p -> p + ":*")
                .collect(Collectors.joining(" & "));

        var toTsQueryFunc = builder.function(
                ToTsQueryFunctionName,
                Object.class,
                ((HibernateCriteriaBuilder) builder).value(searchPattern)
        );

        var searchFunc = builder.function(
                "sql",
                Boolean.class,
                builder.literal("? @@ ?"),
                toTsVectorFunc,
                toTsQueryFunc
        );

        return builder.isTrue(searchFunc);
    }

     */
}
