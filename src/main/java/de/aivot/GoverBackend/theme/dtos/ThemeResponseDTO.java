package de.aivot.GoverBackend.theme.dtos;

import de.aivot.GoverBackend.theme.entities.ThemeEntity;

import java.util.UUID;

public record ThemeResponseDTO(
        String name,
        String main,
        String mainDark,
        String accent,
        String error,
        String warning,
        String info,
        String success,
        UUID logoKey,
        UUID faviconKey
) {
    public static ThemeResponseDTO fromEntity(ThemeEntity theme) {
        return new ThemeResponseDTO(
                theme.getName(),
                theme.getMain(),
                theme.getMainDark(),
                theme.getAccent(),
                theme.getError(),
                theme.getWarning(),
                theme.getInfo(),
                theme.getSuccess(),
                theme.getLogoKey(),
                theme.getFaviconKey()
        );
    }
}
