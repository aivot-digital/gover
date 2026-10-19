package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IdentityCommunicationDerivationRequestDTO(
        @Nonnull
        @NotNull
        Integer bindingId,
        @Nonnull
        @NotNull
        AuthoredElementValues customerData,
        @Nonnull
        @NotNull
        List<String> skipErrorsForElementIds
) {
}
