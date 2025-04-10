package de.aivot.GoverBackend.lib.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;

import javax.annotation.Nonnull;

public interface UpdateEntityService<T, I> extends RetrieveEntityService<T, I> {
    default T update(
            @Nonnull I id,
            @Nonnull T entity
    ) throws ResponseException {
        var existingEntity = retrieve(id);

        if (existingEntity.isEmpty()) {
            throw new ResponseException(HttpStatus.NOT_FOUND, "Entity not found");
        } else {
            return performUpdate(id, entity, existingEntity.get());
        }
    }

    @Nonnull
    T performUpdate(
            @Nonnull I id,
            @Nonnull T entity,
            @Nonnull T existingEntity
    ) throws ResponseException;
}
