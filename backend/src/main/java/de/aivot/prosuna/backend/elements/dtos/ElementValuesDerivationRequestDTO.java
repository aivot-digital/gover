package de.aivot.prosuna.backend.elements.dtos;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

/**
 * Derives authored values against an element tree that the backend loads from a trusted source.
 * <p>
 * The element tree is deliberately not part of the request. Element functions are executed on the server, so clients
 * must not be able to submit their own element trees through endpoints that only check read access.
 */
public record ElementValuesDerivationRequestDTO(
        @Nonnull @NotNull AuthoredElementValues authoredElementValues,
        @Nonnull @NotNull ElementDerivationOptions derivationOptions
) {
}
