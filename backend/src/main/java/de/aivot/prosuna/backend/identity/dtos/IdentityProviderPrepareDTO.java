package de.aivot.prosuna.backend.identity.dtos;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import jakarta.annotation.Nonnull;

public record IdentityProviderPrepareDTO(
        @Nonnull
        @NotNull(message = "Der Endpunkt muss angegeben werden.")
        @URL(message = "Der Endpunkt muss eine gültige URL sein.")
        String endpoint
) {
}
