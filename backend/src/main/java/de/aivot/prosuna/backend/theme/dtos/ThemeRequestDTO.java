package de.aivot.prosuna.backend.theme.dtos;

import de.aivot.prosuna.backend.lib.RequestDTO;
import de.aivot.prosuna.backend.theme.entities.ThemeEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

public record ThemeRequestDTO(
        @Nonnull
        @NotNull(message = "Der Name darf nicht null sein.")
        @Length(min = 3, max = 96, message = "Der Name muss zwischen 3 und 96 Zeichen lang sein.")
        String name,

        @Nonnull
        @NotNull(message = "Die Markenfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Markenfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdefABCDEF]{6}", message = "Die Markenfarbe muss eine gültige HEX-Farbe sein.")
        String primaryColor,

        @Nonnull
        @NotNull(message = "Die Sekundärfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Sekundärfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdefABCDEF]{6}", message = "Die Sekundärfarbe muss eine gültige HEX-Farbe sein.")
        String secondaryColor,

        @Nullable
        @Length(min = 7, max = 7, message = "Die Markenfarbe für das dunkle Farbschema muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdefABCDEF]{6}", message = "Die Markenfarbe für das dunkle Farbschema muss eine gültige HEX-Farbe sein.")
        String primaryColorDark,

        @Nullable
        @Length(min = 7, max = 7, message = "Die Sekundärfarbe für das dunkle Farbschema muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdefABCDEF]{6}", message = "Die Sekundärfarbe für das dunkle Farbschema muss eine gültige HEX-Farbe sein.")
        String secondaryColorDark,

        @Nullable
        UUID logoKey,

        @Nullable
        UUID faviconKey
) implements RequestDTO<ThemeEntity> {
    @Override
    public ThemeEntity toEntity() {
        return new ThemeEntity(
                null,
                name,
                primaryColor,
                secondaryColor,
                primaryColorDark,
                secondaryColorDark,
                logoKey,
                faviconKey
        );
    }
}
