package de.aivot.GoverBackend.system.dtos;

import de.aivot.GoverBackend.theme.dtos.ThemeResponseDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
import jakarta.annotation.Nullable;

public record SystemSetupDTO(
        @Nullable
        String providerName,

        @Nullable
        ThemeResponseDTO providerTheme
) {
}
