package de.aivot.GoverBackend.payment.models;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.elements.models.form.layout.GroupLayout;
import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;
import de.aivot.GoverBackend.payment.exceptions.PaymentException;
import de.aivot.GoverBackend.utils.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public interface PaymentProviderDefinition {
    @Nonnull
    String getKey();

    @Nonnull
    String getProviderName();

    @Nonnull
    String getProviderDescription();

    @Nullable
    GroupLayout getPaymentConfigLayout() throws ResponseException;

    @Nonnull
    default XBezahldienstePaymentRequest createPaymentRequest(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull String purpose,
            @Nonnull String description,
            @Nonnull List<PaymentItem> paymentItems,
            @Nonnull String redirectURL
    ) throws PaymentException {
        // Check that the purpose is not empty
        if (StringUtils.isNullOrEmpty(purpose)) {
            throw new PaymentException("Failed to create payment request. Purpose is empty");
        }

        // Check that the list of paymentItems is not empty
        if (paymentItems.isEmpty()) {
            throw new PaymentException("Failed to create payment request. Products are empty");
        }

        // Transform the list of paymentItems to a list of XBezahldienstePaymentItem
        var xBezahldienstePaymentItems = new LinkedList<XBezahldienstePaymentItem>();
        for (var product : paymentItems) {
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
        request.setDescription(description);
        request.setPurpose(purpose);
        request.setRequestTimestampNow();
        request.setItemsAndCalculateGrosAmount(xBezahldienstePaymentItems);

        request.setRequestor(null); // TODO: Set requester correct on a higher level

        request.setRedirectUrl(redirectURL);

        if (request.getGrosAmount().equals(BigDecimal.ZERO)) {
            throw new PaymentException("Gros amount is 0");
        }

        return request;
    }

    @Nonnull
    XBezahldienstePaymentTransaction initiatePayment(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull XBezahldienstePaymentRequest paymentRequest
    ) throws PaymentException;

    @Nonnull
    XBezahldienstePaymentTransaction onPaymentResultPull(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction
    ) throws PaymentException;

    @Nonnull
    XBezahldienstePaymentTransaction onPaymentResultPush(
            @Nonnull PaymentProviderEntity paymentProviderEntity,
            @Nonnull Map<String, Object> config,
            @Nonnull XBezahldienstePaymentTransaction paymentTransaction,
            @Nonnull Map<String, Object> callbackData
    ) throws PaymentException;
}
