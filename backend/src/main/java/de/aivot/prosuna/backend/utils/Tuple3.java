package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;

public record Tuple3<T, S, Q>(
        @Nonnull T first,
        @Nonnull S second,
        @Nonnull Q third
) {
}
