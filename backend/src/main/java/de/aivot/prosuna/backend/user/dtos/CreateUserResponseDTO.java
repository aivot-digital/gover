package de.aivot.prosuna.backend.user.dtos;

import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record CreateUserResponseDTO(
        @Nonnull UserEntity user,
        boolean initialCredentialsSentByEmail,
        @Nullable String initialCredentialsDeliveryError,
        @Nullable UserInitialCredentialsDTO initialCredentials
) {
}
