package de.aivot.GoverBackend.payment.dtos;

import de.aivot.GoverBackend.enums.XBezahldienstStatus;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentInformation;
import de.aivot.GoverBackend.payment.models.XBezahldienstePaymentRequest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.LocalDateTime;

public record PaymentTransactionResponseDTO(
        @Nonnull String key,
        @Nonnull String paymentProviderKey,
        @Nonnull XBezahldienstePaymentRequest paymentRequest,
        @Nonnull XBezahldienstePaymentInformation paymentInformation,
        @Nullable String paymentError,
        @Nonnull Boolean hasError,
        @Nonnull XBezahldienstStatus status,
        @Nonnull LocalDateTime created
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
