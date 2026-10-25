package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A configurable phase deadline and its provider-defined expiry port. */
public record ProcessNodeDeadlineDefinition(
        @Nonnull
        @NotBlank(message = "Der Schlüssel der Frist darf nicht leer sein.")
        @Size(max = 32, message = "Der Schlüssel der Frist darf maximal 32 Zeichen lang sein.")
        String key,
        @Nonnull
        @NotBlank(message = "Die Beschriftung der Frist darf nicht leer sein.")
        @Size(max = 64, message = "Die Beschriftung der Frist darf maximal 64 Zeichen lang sein.")
        String label,
        @Nonnull
        @NotBlank(message = "Die Beschreibung der Frist darf nicht leer sein.")
        @Size(max = 512, message = "Die Beschreibung der Frist darf maximal 512 Zeichen lang sein.")
        String description,
        @Nonnull @NotNull(message = "Der Prozesspfad für die Fristüberschreitung darf nicht leer sein.") @Valid ProcessNodePort port
) {
}
