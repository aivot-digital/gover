package de.aivot.GoverBackend.nocode.dtos;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.nocode.exceptions.NoCodeException;
import de.aivot.GoverBackend.nocode.models.NoCodeExpression;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

public record NoCodeTestRequestDTO(
        @Nonnull
        @NotNull(message = "Die Element-Daten dürfen nicht null sein.")
        ElementData elementData,

        @Nonnull
        @NotNull(message = "Der Ausdruck darf nicht null sein.")
        NoCodeExpression expression
) {
}
