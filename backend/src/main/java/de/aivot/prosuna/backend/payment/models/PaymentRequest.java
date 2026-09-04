package de.aivot.prosuna.backend.payment.models;

import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Version-independent payment request passed from the application to a provider adapter.
 */
public record PaymentRequest(
        String requestId,
        Instant requestTimestamp,
        String currency,
        BigDecimal grossAmount,
        String purpose,
        @Nullable String description,
        URI redirectUrl,
        List<PaymentRequestItem> items,
        @Nullable PaymentRequestor requestor
) {
    public static final String DEFAULT_CURRENCY = "EUR";

    public PaymentRequest {
        Objects.requireNonNull(requestId, "Payment request ID must not be null");
        Objects.requireNonNull(requestTimestamp, "Payment request timestamp must not be null");
        Objects.requireNonNull(currency, "Payment request currency must not be null");
        Objects.requireNonNull(grossAmount, "Payment request gross amount must not be null");
        Objects.requireNonNull(purpose, "Payment request purpose must not be null");
        Objects.requireNonNull(redirectUrl, "Payment request redirect URL must not be null");
        items = List.copyOf(Objects.requireNonNull(items, "Payment request items must not be null"));
        if (requestId.isBlank() || purpose.isBlank() || items.isEmpty()) {
            throw new IllegalArgumentException("Payment request ID, purpose and items must not be empty");
        }
        if (grossAmount.signum() <= 0) {
            throw new IllegalArgumentException("Payment request gross amount must be positive");
        }
    }
}
