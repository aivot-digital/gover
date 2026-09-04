package de.aivot.prosuna.backend.payment.dtos;

import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;
import de.aivot.prosuna.backend.payment.models.PaymentInformation;
import de.aivot.prosuna.backend.payment.models.PaymentRequest;
import de.aivot.prosuna.backend.payment.models.PaymentStatus;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransactionResponseDTO(
        @Nonnull String key,
        @Nonnull UUID paymentProviderKey,
        @Nonnull PaymentRequest paymentRequest,
        @Nonnull PaymentInformation paymentInformation,
        @Nullable String paymentError,
        @Nonnull Boolean hasError,
        @Nonnull PaymentStatus status,
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
