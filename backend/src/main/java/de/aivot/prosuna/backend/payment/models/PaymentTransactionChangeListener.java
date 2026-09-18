package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;

public interface PaymentTransactionChangeListener {
    void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException;
    void onDelete(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException;
}
