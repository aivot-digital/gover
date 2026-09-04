package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.payment.exceptions.PaymentException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PaymentProviderTestResult(
        @Nonnull Boolean ok,
        @Nullable PaymentRequest request,
        @Nullable PaymentInformation paymentInformation,
        @Nullable String errorMessage
) {
    @Nonnull
    public static PaymentProviderTestResult fromPaymentInformation(
            @Nonnull PaymentRequest request,
            @Nonnull PaymentInformation paymentInformation
    ) {
        return new PaymentProviderTestResult(
                true,
                request,
                paymentInformation,
                null
        );
    }

    @Nonnull
    public static PaymentProviderTestResult fromException(
            @Nullable PaymentRequest request,
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
