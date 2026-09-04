package de.aivot.prosuna.backend.payment.models;

import jakarta.annotation.Nullable;

public record PaymentMethod(String code, @Nullable String detail) {
    public PaymentMethod {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Payment method code must not be blank");
        }
    }

    public String displayName() {
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return switch (code) {
            case "GIROPAY" -> "Giropay";
            case "PAYDIRECT" -> "Paydirekt";
            case "CREDITCARD" -> "Kreditkarte";
            case "PAYPAL" -> "PayPal";
            case "OTHER" -> "Anderes Zahlungsmittel";
            default -> code;
        };
    }
}
