package de.aivot.gover.backend.payment.dtos;

import de.aivot.gover.backend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.gover.backend.payment.models.XBezahldienstePaymentTransaction;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PaymentProviderTestDataResponseDTO(
        @Nonnull Boolean ok,
        @Nullable XBezahldienstePaymentRequest request,
        @Nullable XBezahldienstePaymentTransaction transaction,
        @Nullable String errorMessage
) {
}
