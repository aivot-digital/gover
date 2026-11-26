package de.aivot.prosuna.backend.identity.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IdentityCommunicationDerivationRequestDTO(
        @Nonnull
        @NotNull(message = "Wählen Sie einen Kommunikationsweg aus.")
        Integer bindingId,
        @Nonnull
        @NotNull(message = "Die Angaben zur Identität müssen übermittelt werden.")
        AuthoredElementValues customerData,
        @Nonnull
        @NotNull(message = "Die Liste der von der Prüfung ausgenommenen Formularelemente muss angegeben werden.")
        List<String> skipErrorsForElementIds
) {
}
