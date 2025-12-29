package de.aivot.GoverBackend.payment.dtos;

import de.aivot.GoverBackend.elements.models.ElementData;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;

import jakarta.annotation.Nonnull;
import java.util.UUID;

public record PaymentProviderResponseDTO(
        @Nonnull
        UUID key,
        @Nonnull
        String name,
        @Nonnull
        String description,
        @Nonnull
        String providerKey,
        @Nonnull
        Boolean isTestProvider,
        @Nonnull
        Boolean isEnabled,
        @Nonnull
        ElementData config
) {
    @Nonnull
    public static PaymentProviderResponseDTO fromEntity(
            @Nonnull
            PaymentProviderEntity entity
    ) {
        return new PaymentProviderResponseDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDescription(),
                entity.getPaymentProviderDefinitionKey(),
                entity.getTestProvider(),
                entity.getIsEnabled(),
                entity.getConfig()
        );
    }
}
