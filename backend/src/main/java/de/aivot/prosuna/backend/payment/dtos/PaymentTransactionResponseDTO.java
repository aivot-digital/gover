package de.aivot.prosuna.backend.payment.dtos;

import de.aivot.prosuna.backend.enums.XBezahldienstStatus;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.prosuna.backend.payment.models.XBezahldienstePaymentRequest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionResponseDTO(
        @Nonnull String key,
        @Nonnull UUID paymentProviderKey,
        @Nonnull XBezahldienstePaymentRequest paymentRequest,
        @Nonnull XBezahldienstePaymentInformation paymentInformation,
        @Nullable String paymentError,
        @Nonnull Boolean hasError,
        @Nonnull XBezahldienstStatus status,
        @Nonnull Instant created
        ) {
    public static PaymentTransactionResponseDTO fromEntity(
            @Nonnull PaymentTransactionEntity entity
    ) {
        return new PaymentTransactionResponseDTO(
                entity.getKey(),
                entity.getPaymentProviderKey(),
                entity.getPaymentRequest(),
                entity.getPaymentInformation(),
                entity.getPaymentError(),
                entity.hasError(),
                entity.getStatus(),
                entity.getCreated()
        );
    }
}
