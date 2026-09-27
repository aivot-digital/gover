package de.aivot.prosuna.backend.process.dtos;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record ProcessAssignmentOptionDTO(@Nonnull String id, @Nonnull String name, @Nullable String email) {
}
