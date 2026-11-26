package de.aivot.prosuna.backend.user.dtos;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SetPasswordRequestDTO(
        @Nonnull
        @NotNull(message = "Das Passwort muss angegeben werden.")
        @NotBlank(message = "Das Passwort darf nicht leer sein.")
        @Size(min = 12, max = 128, message = "Das Passwort muss zwischen 12 und 128 Zeichen lang sein.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
                message = "Das Passwort muss Groß- und Kleinbuchstaben (A–Z, a–z), mindestens eine Zahl und mindestens eines der Sonderzeichen @$!%*?& enthalten. Andere Zeichen sind nicht erlaubt."
        )
        String password
) {
}
