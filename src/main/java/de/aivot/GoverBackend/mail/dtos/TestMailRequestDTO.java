package de.aivot.GoverBackend.mail.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nonnull;

public record TestMailRequestDTO(
        @Nonnull
        @NotNull(message = "targetMail must not be null")
        @Email(message = "targetMail must be a valid email address")
        String targetMail
) {
}
