package de.aivot.gover.backend.payment.exceptions;

import de.aivot.gover.backend.payment.entities.PaymentProviderEntity;

public class PaymentSerializationException extends PaymentException {
    public PaymentSerializationException(Throwable throwable, String message, String source, PaymentProviderEntity paymentProviderEntity) {
        super(
                throwable,
                "Failed to serialize payment data for payment provider %s (%s): %s. Source was %s",
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey(),
                message,
                source
        );
    }
}
