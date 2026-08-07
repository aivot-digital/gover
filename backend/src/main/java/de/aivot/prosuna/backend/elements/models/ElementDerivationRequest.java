package de.aivot.prosuna.backend.elements.models;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.process.models.ProcessExecutionData;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record ElementDerivationRequest(
        @Nonnull
        @NotNull(message = "Das Wurzelelement der Ableitung darf nicht leer sein.")
        BaseElement element,
        @Nonnull
        @NotNull(message = "Die Eingabedaten der Formularelemente dürfen nicht leer sein.")
        AuthoredElementValues authoredElementValues,
        @Nonnull
        @NotNull(message = "Die Einstellungen zur Ableitung der Formulardaten dürfen nicht leer sein.")
        ElementDerivationOptions derivationOptions,
        @Nonnull
        @NotNull(message = "Die Ausführungsdaten des Prozesses, in dem die Ableitung stattfindet, dürfen nicht leer sein.")
        ProcessExecutionData processExecutionData
) {
    public ElementDerivationRequest(
            @Nonnull
            BaseElement element,
            @Nonnull
            AuthoredElementValues authoredElementValues,
            @Nonnull
            ElementDerivationOptions derivationOptions
    ) {
        this(element, authoredElementValues, derivationOptions, new ProcessExecutionData());
    }

    public ElementDerivationRequest(
            @Nonnull
            BaseElement element,
            @Nonnull
            AuthoredElementValues authoredElementValues
    ) {
        this(element, authoredElementValues, new ElementDerivationOptions(), new ProcessExecutionData());
    }

    public ElementDerivationRequest(
            @Nonnull
            BaseElement element,
            @Nonnull
            AuthoredElementValues authoredElementValues,
            @Nonnull
            ProcessExecutionData additionalDerivationData
    ) {
        this(element, authoredElementValues, new ElementDerivationOptions(), additionalDerivationData);
    }
}
