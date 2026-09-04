package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.prosuna.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;
import de.aivot.prosuna.backend.payment.exceptions.PaymentException;
import de.aivot.prosuna.backend.plugin.enums.PluginComponentType;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public interface PaymentProviderDefinition extends PluginComponent {
    @Nonnull
    @Override
    default PluginComponentType getComponentType() {
        return PluginComponentType.PaymentProviderDefinition;
    }

    @Nonnull
    String getProviderName();

    @Nonnull
    String getProviderDescription();

    @Nullable
    GroupLayoutElement getPaymentConfigLayout() throws ResponseException;

    @Nonnull
    default PaymentRequest createPaymentRequest(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentPayload payload,
            @Nonnull String redirectURL
    ) throws PaymentException {
        // Check that the purpose is not empty
        if (StringUtils.isNullOrEmpty(payload.getPurpose())) {
            throw new PaymentException("Failed to create payment request. Purpose is empty");
        }

        // Check that the list of paymentItems is not empty
        if (payload.getPaymentItems().isEmpty()) {
            throw new PaymentException("Failed to create payment request. Products are empty");
        }

        var paymentRequestItems = new LinkedList<PaymentRequestItem>();
        for (var product : payload.getPaymentItems()) {
            product
                    .toPaymentRequestItem()
                    .ifPresent(paymentRequestItems::add);
        }

        if (paymentRequestItems.isEmpty()) {
            throw new PaymentException("No items for payment request");
        }

        var grossAmount = paymentRequestItems.stream()
                .map(PaymentRequestItem::grossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (grossAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new PaymentException("Gross amount is 0");
        }

        try {
            return new PaymentRequest(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    PaymentRequest.DEFAULT_CURRENCY,
                    grossAmount,
                    payload.getPurpose(),
                    payload.getDescription(),
                    URI.create(redirectURL),
                    paymentRequestItems,
                    payload.getRequestor()
            );
        } catch (IllegalArgumentException e) {
            throw new PaymentException(e, "Failed to create payment request");
        }
    }

    @Nonnull
    PaymentInformation initiatePayment(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentRequest paymentRequest
    ) throws PaymentException;

    @Nonnull
    PaymentInformation onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentInformation paymentInformation
    ) throws PaymentException;

    @Nonnull
    PaymentInformation onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull PaymentInformation paymentInformation,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException;
}
