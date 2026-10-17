package de.aivot.prosuna.backend.utils;

import jakarta.annotation.Nonnull;

public record Tuple4<T, S, Q, W>(
        @Nonnull T first,
        @Nonnull S second,
        @Nonnull Q third,
        @Nonnull W fourth
) {
}
