package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record ProcessNodeConfigurationDerivationRequest(
        @Nonnull @NotNull AuthoredElementValues authoredElementValues,
        @Nonnull @NotNull ElementDerivationOptions derivationOptions
) {
}
