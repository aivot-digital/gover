package de.aivot.prosuna.backend.payment.dtos;

import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentRequest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PaymentProviderTestDataResponseDTO(
        @Nonnull Boolean ok,
        @Nullable PaymentRequest request,
        @Nullable PaymentInformation paymentInformation,
        @Nullable String errorMessage
) {
}
