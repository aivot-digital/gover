package de.aivot.prosuna.backend.payment.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import jakarta.annotation.Nonnull;
import java.math.BigDecimal;

public record PaymentProviderTestDataRequestDTO(
        @Nonnull
        @NotNull(message = "Geben Sie einen Verwendungszweck ein.")
        @NotBlank(message = "Geben Sie einen Verwendungszweck ein.")
        String purpose,

        @Nonnull
        @NotNull(message = "Geben Sie eine Beschreibung ein.")
        @NotBlank(message = "Geben Sie eine Beschreibung ein.")
        String description,

        @Nonnull
        @NotNull(message = "Geben Sie einen Einzelpreis (netto) ein.")
        BigDecimal amount
) {
}
