package de.aivot.prosuna.backend.customLink.dtos;

import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CustomLinkOrderRequestDTO(
        @NotNull(message = "Wählen Sie einen Typ für die Custom-Links aus.") CustomLinkType type,
        @NotNull(message = "Die Reihenfolge der Custom-Links muss angegeben werden.") @NotEmpty(message = "Die Reihenfolge muss mindestens einen Custom-Link enthalten.") List<@NotNull(message = "Die Reihenfolge enthält einen ungültigen Custom-Link.") Integer> ids
) {
}
