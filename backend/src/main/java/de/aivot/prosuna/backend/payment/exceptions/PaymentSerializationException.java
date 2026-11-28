package de.aivot.prosuna.backend.payment.exceptions;

import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;

public class PaymentSerializationException extends PaymentException {
    public PaymentSerializationException(Throwable throwable, String message, String source, PaymentProviderEntity paymentProviderEntity) {
        super(
                throwable,
                "Die Zahlungsdaten für den Zahlungsanbieter %s (%s) konnten nicht verarbeitet werden. Fehlermeldung: %s. Quelldaten: %s",
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey(),
                message,
                source
        );
    }
}
