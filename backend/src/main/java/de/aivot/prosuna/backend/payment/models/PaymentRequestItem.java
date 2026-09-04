package de.aivot.prosuna.backend.payment.models;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

public record PaymentRequestItem(
        String id,
        String reference,
        String description,
        BigDecimal taxRate,
        long quantity,
        BigDecimal totalNetAmount,
        BigDecimal totalTaxAmount,
        BigDecimal singleNetAmount,
        BigDecimal singleTaxAmount,
        Map<String, String> bookingData
) {
    public PaymentRequestItem {
        Objects.requireNonNull(id, "Payment item ID must not be null");
        Objects.requireNonNull(reference, "Payment item reference must not be null");
        Objects.requireNonNull(description, "Payment item description must not be null");
        Objects.requireNonNull(taxRate, "Payment item tax rate must not be null");
        Objects.requireNonNull(totalNetAmount, "Payment item total net amount must not be null");
        Objects.requireNonNull(totalTaxAmount, "Payment item total tax amount must not be null");
        Objects.requireNonNull(singleNetAmount, "Payment item single net amount must not be null");
        Objects.requireNonNull(singleTaxAmount, "Payment item single tax amount must not be null");
        bookingData = bookingData == null ? Map.of() : Map.copyOf(bookingData);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Payment item quantity must be positive");
        }
    }

    public BigDecimal grossAmount() {
        return totalNetAmount.add(totalTaxAmount);
    }
}
