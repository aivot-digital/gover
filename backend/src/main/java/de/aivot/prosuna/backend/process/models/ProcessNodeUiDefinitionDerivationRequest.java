package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Derives a UI definition configured in a UI definition field of a process node.
 *
 * @param fieldId               the ID of the UI definition field in the node's configuration layout
 * @param uiDefinition          the unsaved UI definition, or {@code null} to derive the stored UI definition
 * @param authoredElementValues the values entered into the UI definition
 * @param derivationOptions     the options for the derivation
 */
public record ProcessNodeUiDefinitionDerivationRequest(
        @Nonnull @NotBlank String fieldId,
        @Nullable BaseElement uiDefinition,
        @Nonnull @NotNull AuthoredElementValues authoredElementValues,
        @Nonnull @NotNull ElementDerivationOptions derivationOptions
) {
}
