package de.aivot.prosuna.backend.payment.models;

import jakarta.annotation.Nullable;

import java.util.List;

public record PaymentAddress(
        @Nullable String street,
        @Nullable String houseNumber,
        List<String> addressLines,
        @Nullable String postalCode,
        @Nullable String city,
        @Nullable String country
) {
    public PaymentAddress {
        addressLines = addressLines == null ? List.of() : List.copyOf(addressLines);
    }
}
