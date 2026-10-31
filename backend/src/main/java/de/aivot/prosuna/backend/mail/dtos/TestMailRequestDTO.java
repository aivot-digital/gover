package de.aivot.prosuna.backend.mail.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import jakarta.annotation.Nonnull;

public record TestMailRequestDTO(
        @Nonnull
        @NotNull(message = "Geben Sie eine E-Mail-Adresse ein.")
        @Email(message = "Geben Sie eine gültige E-Mail-Adresse ein.")
        String targetMail
) {
}
