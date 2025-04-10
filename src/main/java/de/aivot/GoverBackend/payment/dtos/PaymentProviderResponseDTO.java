package de.aivot.GoverBackend.payment.dtos;

import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;

import javax.annotation.Nonnull;
import java.util.Map;

public record PaymentProviderResponseDTO(
        @Nonnull
        String key,
        @Nonnull
        String name,
        @Nonnull
        String description,
        @Nonnull
        String providerKey,
        @Nonnull
        Boolean isTestProvider,
        @Nonnull
        Map<String, Object> config
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
                entity.getProviderKey(),
                entity.getTestProvider(),
                entity.getConfig()
        );
    }
}
