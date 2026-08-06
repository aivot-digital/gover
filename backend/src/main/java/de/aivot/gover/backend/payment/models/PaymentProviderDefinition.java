package de.aivot.gover.backend.payment.models;

import de.aivot.gover.backend.elements.models.DerivedRuntimeElementData;
import de.aivot.gover.backend.elements.models.elements.layout.GroupLayoutElement;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;
import de.aivot.gover.backend.payment.exceptions.PaymentException;
import de.aivot.gover.backend.plugin.enums.PluginComponentType;
import de.aivot.gover.backend.plugin.models.PluginComponent;
import de.aivot.gover.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    default XBezahldienstePaymentRequest createPaymentRequest(
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

        // Transform the list of paymentItems to a list of XBezahldienstePaymentItem
        var xBezahldienstePaymentItems = new LinkedList<XBezahldienstePaymentItem>();
        for (var product : payload.getPaymentItems()) {
            product
                    .toXBezahldienstePaymentItem()
                    .ifPresent(xBezahldienstePaymentItems::add);
        }

        // Check that the list of XBezahldienstePaymentItem is not empty
        if (xBezahldienstePaymentItems.isEmpty()) {
            throw new PaymentException("No items for payment request");
        }

        // Construct the payment request
        var request = new XBezahldienstePaymentRequest();
        request.setRandomRequestId();
        request.setDescription(payload.getDescription());
        request.setPurpose(payload.getPurpose());
        request.setRequestTimestampNow();
        request.setItemsAndCalculateGrosAmount(xBezahldienstePaymentItems);
        request.setRequestor(payload.getRequestor());

        request.setRedirectUrl(redirectURL);

        if (request.getGrosAmount().equals(BigDecimal.ZERO)) {
            throw new PaymentException("Gros amount is 0");
        }

        return request;
    }

    @Nonnull
    XBezahldienstePaymentTransaction initiatePayment(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull XBezahldienstePaymentRequest paymentRequest
    ) throws PaymentException;

    @Nonnull
    XBezahldienstePaymentTransaction onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction
    ) throws PaymentException;

    @Nonnull
    XBezahldienstePaymentTransaction onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull DerivedRuntimeElementData config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException;
}
