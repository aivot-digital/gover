package de.aivot.gover.backend.search.dtos;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;

public record SearchRecentItemRequestDTO(
        @Nonnull
        @NotBlank
        String id,
        @Nonnull
        @NotBlank
        String originTable
) {
}
