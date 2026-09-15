package de.aivot.prosuna.backend.system.dtos;

import de.aivot.prosuna.backend.theme.dtos.ThemeResponseDTO;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public record SystemSetupDTO(
        @Nullable
        String providerName,

        @Nullable
        ThemeResponseDTO providerTheme,

        @Nonnull
        Map<String, Object> publicConfigs
) {
}
