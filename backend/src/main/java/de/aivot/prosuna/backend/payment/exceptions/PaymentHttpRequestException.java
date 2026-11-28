package de.aivot.prosuna.backend.payment.exceptions;

import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;

public class PaymentHttpRequestException extends PaymentException {
    public PaymentHttpRequestException(Throwable throwable, PaymentProviderEntity paymentProviderEntity, String requestBody) {
        super(
                throwable,
                "Die Anfrage an den Zahlungsanbieter %s (%s) konnte nicht gesendet werden. Anfragedaten: %s",
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey(),
                requestBody
        );
    }

    public PaymentHttpRequestException(int status, PaymentProviderEntity paymentProviderEntity, String requestBody, String responseBody) {
        super(
                "Die Anfrage an den Zahlungsanbieter %s (%s) ist fehlgeschlagen. HTTP-Statuscode: %d. Anfragedaten: %s. Antwortdaten: %s",
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey(),
                status,
                requestBody,
                responseBody
        );
    }
}
