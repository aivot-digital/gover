package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nonnull;

public record ProcessInstanceAccessSelectableItem(
        @Nonnull String type,
        @Nonnull String id
) {
}
