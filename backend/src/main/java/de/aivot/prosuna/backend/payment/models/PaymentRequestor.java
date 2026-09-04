package de.aivot.prosuna.backend.payment.models;

import jakarta.annotation.Nullable;

public record PaymentRequestor(
        @Nullable String name,
        @Nullable String firstName,
        @Nullable PaymentGender gender,
        boolean organization,
        @Nullable String organizationName,
        @Nullable PaymentAddress address
) {
}
