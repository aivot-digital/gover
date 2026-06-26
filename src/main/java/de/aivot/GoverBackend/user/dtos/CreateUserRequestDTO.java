package de.aivot.GoverBackend.user.dtos;

import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequestDTO(
        @Nonnull
        @NotNull(message = "Die Mitarbeiter:in darf nicht null sein.")
        @Valid
        UserEntity user,

        @Nonnull
        @NotNull(message = "Die Versandoption für initiale Zugangsdaten darf nicht null sein.")
        Boolean sendInitialCredentialsByEmail
) {
}
