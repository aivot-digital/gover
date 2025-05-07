package de.aivot.GoverBackend.utils.specification;

import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class SpecificationBuilder<T> {
    private final List<SpecificationBuilderItem<T>> items = new LinkedList<>();

    private SpecificationBuilder() {
    }

    @Nonnull
    public static <T> SpecificationBuilder<T> create() {
        return new SpecificationBuilder<>();
    }

    @Nonnull
    public static <T> SpecificationBuilder<T> create(@Nonnull Class<T> ignored) {
        return new SpecificationBuilder<>();
    }

    @Nonnull
    public SpecificationBuilder<T> withEquals(@Nonnull String field, @Nullable Object value) {
        if (value == null) {
            return this;
        }

        return with(new SpecificationBuilderEquals<>(field, value));
    }

    @Nonnull
    public SpecificationBuilder<T> withJsonEquals(@Nonnull String field, @Nonnull List<String> path, @Nullable String value) {
        if (value == null) {
            return this;
        }

        return with(new SpecificationBuilderJsonEquals<>(field, path, value));
    }

    @Nonnull
    public SpecificationBuilder<T> withNotEquals(@Nonnull String field, @Nullable Object value) {
        if (value == null) {
            return this;
        }

        return with(new SpecificationBuilderNotEquals<>(field, value));
    }

    @Nonnull
    public SpecificationBuilder<T> withContains(@Nonnull String field, @Nullable String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return this;
        }

        return with(new SpecificationBuilderContains<>(field, value));
    }

    @Nonnull
    public SpecificationBuilder<T> withInList(@Nonnull String field, @Nullable Collection<?> value) {
        if (value == null || value.isEmpty()) {
            return this;
        }

        return with(new SpecificationBuilderInList<>(field, value));
    }

    @Nonnull
    public SpecificationBuilder<T> withNull(@Nonnull String field) {
        return with(new SpecificationBuilderIsNull<>(field));
    }

    @Nonnull
    public SpecificationBuilder<T> withNotNull(@Nonnull String field) {
        return with(new SpecificationBuilderIsNotNull<>(field));
    }

    public SpecificationBuilder<T> withJsonArrayElementFieldEquals(@Nonnull String field, @Nonnull String elementField, @Nonnull String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return this;
        }

        return with(new SpecificationBuilderJsonArrayElementFieldEquals<>(field, elementField, value));
    }

    @Nonnull
    private SpecificationBuilder<T> with(@Nonnull SpecificationBuilderItem<T> item) {
        items.add(item);
        return this;
    }

    @Nonnull
    public Specification<T> build() {
        return (root, query, builder) -> {
            var predicates = items
                    .stream()
                    .map(entry -> entry.toPredicate(root, query, builder))
                    .toArray(Predicate[]::new);

            return builder.and(predicates);
        };
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
