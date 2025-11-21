package de.aivot.GoverBackend.lib.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.http.HttpStatus;

import javax.annotation.Nonnull;

public interface DeleteEntityService<T, I> extends RetrieveEntityService<T, I> {
    default T delete(
            @Nonnull I id
    ) throws ResponseException {
        var entity = retrieve(id);
        if (entity.isEmpty()) {
            throw new ResponseException(HttpStatus.NOT_FOUND, "Entity not found");
        }
        performDelete(entity.get());
        return entity.get();
    }

    default T deleteEntity(
            @Nonnull T entity
    ) throws ResponseException {
        performDelete(entity);
        return entity;
    }

    void performDelete(
            @Nonnull T entity
    ) throws ResponseException;
}
