package de.aivot.GoverBackend.lib.services;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;

import javax.annotation.Nonnull;

public interface CreateEntityService<T> {
    @Nonnull
    T create(
            @Nonnull T entity
    ) throws ResponseException;
}
