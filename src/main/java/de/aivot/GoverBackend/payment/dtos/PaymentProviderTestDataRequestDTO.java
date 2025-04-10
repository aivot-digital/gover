package de.aivot.GoverBackend.payment.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import javax.annotation.Nonnull;
import java.math.BigDecimal;

public record PaymentProviderTestDataRequestDTO(
        @Nonnull
        @NotNull(message = "purpose is required")
        @NotBlank(message = "purpose cannot be blank")
        String purpose,

        @Nonnull
        @NotNull(message = "description is required")
        @NotBlank(message = "description cannot be blank")
        String description,

        @Nonnull
        @NotNull(message = "amount is required")
        BigDecimal amount
) {
}
