package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record IdentityCommunicationSelectionRequestDTO(
        @Nonnull
        @NotNull
        Integer bindingId,
        @Nonnull
        @NotNull
        AuthoredElementValues customerData
) {
}
