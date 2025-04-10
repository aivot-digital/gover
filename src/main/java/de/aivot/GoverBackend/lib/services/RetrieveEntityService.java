package de.aivot.GoverBackend.lib.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.lib.models.Filter;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface RetrieveEntityService<T, I> {
    @Nonnull
    Optional<T> retrieve(
            @Nonnull I id
    ) throws ResponseException;

    default Optional<T> retrieve(
            @Nonnull Filter<T> filter
    ) throws ResponseException {
        return retrieve(filter.build());
    }

    @Nonnull
    Optional<T> retrieve(
            @Nonnull Specification<T> specification
    ) throws ResponseException;

    boolean exists(
            @Nonnull I id
    );

    default boolean exists(
            @Nonnull Filter<T> filter
    ) {
        return exists(filter.build());
    }

    boolean exists(
            @Nonnull Specification<T> specification
    );

    default boolean notExists(
            @Nonnull I id
    ) {
        return !exists(id);
    }

    default boolean notExists(
            @Nonnull Filter<T> filter
    ) {
        return notExists(filter.build());
    }

    default boolean notExists(
            @Nonnull Specification<T> specification
    ) {
        return !exists(specification);
    }
}
