package de.aivot.prosuna.backend.search.dtos;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;

public record SearchRecentItemRequestDTO(
        @Nonnull
        @NotBlank(message = "Die Kennung des zuletzt geöffneten Eintrags muss angegeben werden.")
        String id,
        @Nonnull
        @NotBlank(message = "Die Art des zuletzt geöffneten Eintrags muss angegeben werden.")
        String originTable
) {
}
