package de.aivot.GoverBackend.user.dtos;

import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record CreateUserResponseDTO(
        @Nonnull UserEntity user,
        boolean initialCredentialsSentByEmail,
        @Nullable String initialCredentialsDeliveryError,
        @Nullable UserInitialCredentialsDTO initialCredentials
) {
}
