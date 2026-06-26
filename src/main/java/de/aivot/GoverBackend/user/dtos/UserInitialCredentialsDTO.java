package de.aivot.GoverBackend.user.dtos;

import jakarta.annotation.Nonnull;

public record UserInitialCredentialsDTO(
        @Nonnull String fullName,
        @Nonnull String email,
        @Nonnull String systemRoleName,
        @Nonnull String temporaryPassword
) {
}
