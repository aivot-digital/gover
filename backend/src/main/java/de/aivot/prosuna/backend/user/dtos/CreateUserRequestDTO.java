package de.aivot.prosuna.backend.user.dtos;

import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequestDTO(
        @Nonnull
        @NotNull(message = "Die Kontodaten müssen angegeben werden.")
        @Valid
        UserEntity user,

        @Nonnull
        @NotNull(message = "Geben Sie an, ob die ersten Zugangsdaten per E-Mail versendet werden sollen.")
        Boolean sendInitialCredentialsByEmail
) {
}
