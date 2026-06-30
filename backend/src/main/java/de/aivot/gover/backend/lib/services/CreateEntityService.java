package de.aivot.gover.backend.lib.services;

import de.aivot.gover.backend.lib.exceptions.ResponseException;

import jakarta.annotation.Nonnull;

public interface CreateEntityService<T> {
    @Nonnull
    T create(
            @Nonnull T entity
    ) throws ResponseException;
}
