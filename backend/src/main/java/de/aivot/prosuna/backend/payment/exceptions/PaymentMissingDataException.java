package de.aivot.prosuna.backend.payment.exceptions;

import de.aivot.prosuna.backend.payment.entities.PaymentProviderEntity;

public class PaymentMissingDataException extends PaymentException {
    public PaymentMissingDataException(String missingField, PaymentProviderEntity paymentProviderEntity) {
        super(
                "Die Angabe „%s“ für den Zahlungsanbieter %s (%s) fehlt oder ist leer.",
                missingField,
                paymentProviderEntity.getName(),
                paymentProviderEntity.getKey()
        );
    }
}
