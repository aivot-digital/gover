package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record IdentityCommunicationSelectionRequestDTO(
        @Nonnull
        @NotNull(message = "Wählen Sie einen Kommunikationsweg aus.")
        Integer bindingId,
        @Nonnull
        @NotNull(message = "Die Angaben zur Identität müssen übermittelt werden.")
        AuthoredElementValues customerData
) {
}
