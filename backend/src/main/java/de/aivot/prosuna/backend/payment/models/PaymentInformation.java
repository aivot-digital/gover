package de.aivot.prosuna.backend.payment.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

/**
 * Provider- and XBezahldienste-version-independent state of a payment.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record PaymentInformation(
        String providerTransactionId,
        @Nullable String providerReference,
        PaymentStatus status,
        @Nullable URI paymentUrl,
        @Nullable Instant paidAt,
        @Nullable PaymentMethod paymentMethod,
        @Nullable String statusMessage
) {
    public PaymentInformation {
        if (providerTransactionId == null || providerTransactionId.isBlank()) {
            throw new IllegalArgumentException("Provider transaction ID must not be blank");
        }
        Objects.requireNonNull(status, "Payment status must not be null");
        if (status.isPending() && paymentUrl == null) {
            throw new IllegalArgumentException("Pending payments require a payment URL");
        }
        if (status.isTerminal() && paymentUrl != null) {
            throw new IllegalArgumentException("Terminal payments must not have a payment URL");
        }
        if (!status.isPaid() && (paidAt != null || paymentMethod != null)) {
            throw new IllegalArgumentException("Payment date and method are only valid for paid payments");
        }
    }
}
