package de.aivot.GoverBackend.theme.dtos;

import de.aivot.GoverBackend.lib.RequestDTO;
import de.aivot.GoverBackend.theme.entities.ThemeEntity;
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
        @NotNull(message = "Die Hauptfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Hauptfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Hauptfarbe muss eine gültige HEX-Farbe sein.")
        String main,

        @Nonnull
        @NotNull(message = "Die Haupt-Dunkel-Farbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Haupt-Dunkel-Farbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Haupt-Dunkel-Farbe muss eine gültige HEX-Farbe sein.")
        String mainDark,

        @Nonnull
        @NotNull(message = "Die Akzentfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Akzentfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Akzentfarbe muss eine gültige HEX-Farbe sein.")
        String accent,

        @Nonnull
        @NotNull(message = "Die Fehlerfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Fehlerfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Fehlerfarbe muss eine gültige HEX-Farbe sein.")
        String error,

        @Nonnull
        @NotNull(message = "Die Warnfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Warnfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Warnfarbe muss eine gültige HEX-Farbe sein.")
        String warning,

        @Nonnull
        @NotNull(message = "Die Infofarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Infofarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Infofarbe muss eine gültige HEX-Farbe sein.")
        String info,

        @Nonnull
        @NotNull(message = "Die Erfolgsfarbe darf nicht null sein.")
        @Length(min = 7, max = 7, message = "Die Erfolgsfarbe muss eine gültige HEX-Farbe sein.")
        @Pattern(regexp = "#[0-9abcdef]{6}", message = "Die Erfolgsfarbe muss eine gültige HEX-Farbe sein.")
        String success,

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
                main,
                mainDark,
                accent,
                error,
                warning,
                info,
                success,
                logoKey,
                faviconKey
        );
    }
}
