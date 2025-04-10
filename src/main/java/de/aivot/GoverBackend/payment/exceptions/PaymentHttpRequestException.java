package de.aivot.GoverBackend.payment.exceptions;

import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;

public class PaymentHttpRequestException extends PaymentException {
    public PaymentHttpRequestException(Throwable throwable, PaymentProviderEntity paymentProviderEntity, String requestBody) {
        super(
                throwable,
                "Failed to send request for payment provider %s (%s). Request body was %s",
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey(),
                requestBody
        );
    }

    public PaymentHttpRequestException(int status, PaymentProviderEntity paymentProviderEntity, String requestBody, String responseBody) {
        super(
                "Failed to send request for payment provider %s (%s). Endpoint returned status %d Request body was %s. Request body was %s",
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey(),
                status,
                requestBody,
                responseBody
        );
    }
}
