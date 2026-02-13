package de.aivot.GoverBackend.identity.dtos;

import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import jakarta.annotation.Nonnull;

public record IdentityProviderPrepareDTO(
        @Nonnull
        @NotNull(message = "Der Endpoint muss angegeben werden.")
        @URL(message = "Der Endpoint muss eine gültige URL sein.")
        String endpoint
) {
}
