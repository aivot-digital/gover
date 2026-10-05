package de.aivot.prosuna.backend.lib.services;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;

import jakarta.annotation.Nonnull;

public interface CreateEntityService<T> {
    @Nonnull
    T create(
            @Nonnull T entity
    ) throws ResponseException;
}
