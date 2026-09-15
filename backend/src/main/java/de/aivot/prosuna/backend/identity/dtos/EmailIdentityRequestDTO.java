package de.aivot.prosuna.backend.identity.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailIdentityRequestDTO(
        @NotBlank
        @Size(max = 254)
        String emailAddress
) {
}
