package de.aivot.GoverBackend.system.dtos;

import de.aivot.GoverBackend.theme.entities.Theme;
import jakarta.annotation.Nullable;

public record SystemSetupDTO(
        @Nullable
        String providerName,

        @Nullable
        Theme providerTheme
) {
}
