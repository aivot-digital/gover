package de.aivot.GoverBackend.payment.exceptions;

import de.aivot.GoverBackend.payment.entities.PaymentProviderEntity;

public class PaymentMissingDataException extends PaymentException {
    public PaymentMissingDataException(String missingField, PaymentProviderEntity paymentProviderEntity) {
        super(
                "Field '%s' for payment provider %s (%s) is missing or empty",
                missingField,
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey()
        );
    }
}
