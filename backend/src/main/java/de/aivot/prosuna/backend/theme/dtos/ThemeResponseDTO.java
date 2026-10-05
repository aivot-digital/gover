package de.aivot.prosuna.backend.theme.dtos;

import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import jakarta.annotation.Nullable;

import java.util.UUID;

public record ThemeResponseDTO(
        Integer id,
        String name,
        String primaryColor,
        String secondaryColor,
        @Nullable
        String primaryColorDark,
        @Nullable
        String secondaryColorDark,
        @Nullable
        UUID logoKey,
        @Nullable
        UUID logoKeyDark,
        @Nullable
        UUID faviconKey
) {
    public static ThemeResponseDTO fromEntity(ThemeEntity theme) {
        return new ThemeResponseDTO(
                theme.getId(),
                theme.getName(),
                theme.getPrimaryColor(),
                theme.getSecondaryColor(),
                theme.getPrimaryColorDark(),
                theme.getSecondaryColorDark(),
                theme.getLogoKey(),
                theme.getLogoKeyDark(),
                theme.getFaviconKey()
        );
    }
}
