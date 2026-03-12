package de.aivot.GoverBackend.elements.models;

import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record ElementDerivationRequestV2(
        @Nonnull
        @NotNull(message = "Das Wurzelelement der Ableitung darf nicht leer sein.")
        BaseElement element,
        @Nonnull
        @NotNull(message = "Die Eingabedaten der Formularelemente dürfen nicht leer sein.")
        AuthoredElementValues authoredElementValues,
        @Nonnull
        @NotNull(message = "Die Einstellungen zur Ableitung der Formulardaten dürfen nicht leer sein.")
        ElementDerivationOptions derivationOptions
) {

}
