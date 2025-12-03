package de.aivot.GoverBackend.payment.dtos;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.elements.form.layout.GroupLayout;
import de.aivot.GoverBackend.payment.models.PaymentProviderDefinition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PaymentProviderDefinitionResponseDTO(
        @Nonnull
        String key,
        @Nonnull
        String name,
        @Nonnull
        String description,
        @Nullable
        GroupLayout configLayout
) {
    @Nonnull
    public static PaymentProviderDefinitionResponseDTO from(
            @Nonnull
            PaymentProviderDefinition definition
    ) throws ResponseException {
        return new PaymentProviderDefinitionResponseDTO(
                definition.getKey(),
                definition.getProviderName(),
                definition.getProviderDescription(),
                definition.getPaymentConfigLayout()
        );
    }
}
