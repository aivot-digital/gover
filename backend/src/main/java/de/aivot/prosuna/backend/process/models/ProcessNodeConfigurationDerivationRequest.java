package de.aivot.prosuna.backend.process.models;

import de.aivot.prosuna.backend.elements.models.AuthoredElementValues;
import de.aivot.prosuna.backend.elements.models.ElementDerivationOptions;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record ProcessNodeConfigurationDerivationRequest(
        @Nonnull @NotNull(message = "Die Eingabedaten der Formularelemente müssen angegeben werden.") AuthoredElementValues authoredElementValues,
        @Nonnull @NotNull(message = "Die Einstellungen zur Ableitung der Formulardaten müssen angegeben werden.") ElementDerivationOptions derivationOptions
) {
}
