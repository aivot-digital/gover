package de.aivot.prosuna.backend.identity.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailIdentityRequestDTO(
        @NotBlank(message = "Geben Sie eine E-Mail-Adresse ein.")
        @Size(max = 254, message = "Die E-Mail-Adresse darf höchstens 254 Zeichen lang sein.")
        String emailAddress
) {
}
