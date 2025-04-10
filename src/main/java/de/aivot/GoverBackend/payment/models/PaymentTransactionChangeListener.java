package de.aivot.GoverBackend.payment.models;

import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.payment.entities.PaymentTransactionEntity;

public interface PaymentTransactionChangeListener {
    void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException;
}
