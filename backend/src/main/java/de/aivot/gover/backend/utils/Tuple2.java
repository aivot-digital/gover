package de.aivot.gover.backend.utils;

import jakarta.annotation.Nonnull;

public record Tuple2<T, S>(
        @Nonnull T first,
        @Nonnull S second
) {
}
