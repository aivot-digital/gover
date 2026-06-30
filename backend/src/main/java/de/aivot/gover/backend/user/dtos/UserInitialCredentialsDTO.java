package de.aivot.gover.backend.user.dtos;

import jakarta.annotation.Nonnull;

public record UserInitialCredentialsDTO(
        @Nonnull String fullName,
        @Nonnull String email,
        @Nonnull String systemRoleName,
        @Nonnull String temporaryPassword
) {
}
