package de.aivot.GoverBackend.payment.dtos;

import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentTransaction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record PaymentProviderTestDataResponseDTO(
        @Nonnull Boolean ok,
        @Nullable XBezahldienstePaymentRequest request,
        @Nullable XBezahldienstePaymentTransaction transaction,
        @Nullable String errorMessage
) {
}
