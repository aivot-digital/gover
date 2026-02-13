package de.aivot.GoverBackend.payment.models;

import de.aivot.GoverBackend.payment.exceptions.PaymentException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PaymentProviderTestResult(
        @Nonnull Boolean ok,
        @Nullable XBezahldienstePaymentRequest request,
        @Nullable XBezahldienstePaymentTransaction transaction,
        @Nullable String errorMessage
) {
    @Nonnull
    public static PaymentProviderTestResult fromTransaction(
            @Nonnull XBezahldienstePaymentRequest request,
            @Nonnull XBezahldienstePaymentTransaction transaction
    ) {
        return new PaymentProviderTestResult(
                true,
                request,
                transaction,
                null
        );
    }

    @Nonnull
    public static PaymentProviderTestResult fromException(
            @Nullable XBezahldienstePaymentRequest request,
            @Nonnull PaymentException e
    ) {
        return new PaymentProviderTestResult(
                false,
                request,
                null,
                e.getMessage()
        );
    }
}
