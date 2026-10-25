package de.aivot.prosuna.backend.process.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProcessNodePort (
        @NotNull(message = "Der Schlüssel des Prozesspfads darf nicht null sein.")
        @NotBlank(message = "Der Schlüssel des Prozesspfads darf nicht leer sein.")
        @Size(max = 32, message = "Der Schlüssel des Prozesspfads darf maximal 32 Zeichen lang sein.")
        @Nonnull String key,
        @NotNull(message = "Die Beschriftung des Prozesspfads darf nicht null sein.")
        @NotBlank(message = "Die Beschriftung des Prozesspfads darf nicht leer sein.")
        @Size(max = 64, message = "Die Beschriftung des Prozesspfads darf maximal 64 Zeichen lang sein.")
        @Nonnull String label,
        @NotNull(message = "Die Beschreibung des Prozesspfads darf nicht null sein.")
        @NotBlank(message = "Die Beschreibung des Prozesspfads darf nicht leer sein.")
        @Size(max = 512, message = "Die Beschreibung des Prozesspfads darf maximal 512 Zeichen lang sein.")
        @Nonnull String description,
        @Nullable @Size(max = 32, message = "Der Schlüssel der Frist darf maximal 32 Zeichen lang sein.") String deadlineKey
) {
    public ProcessNodePort(@Nonnull String key, @Nonnull String label, @Nonnull String description) {
        this(key, label, description, null);
    }
}
