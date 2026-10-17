package de.aivot.prosuna.backend.customLink.dtos;

import de.aivot.prosuna.backend.customLink.enums.CustomLinkType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CustomLinkOrderRequestDTO(
        @NotNull CustomLinkType type,
        @NotNull @NotEmpty List<@NotNull Integer> ids
) {
}
