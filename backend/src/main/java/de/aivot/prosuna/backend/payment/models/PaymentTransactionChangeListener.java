package de.aivot.prosuna.backend.payment.models;

import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.payment.entities.PaymentTransactionEntity;

/**
 * Reacts to changes of payment transactions. Listeners run in the transaction that persists the change, so the work
 * they request commits together with it; throwing rolls back the change.
 */
public interface PaymentTransactionChangeListener {
    void onChange(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException;
    void onDelete(PaymentTransactionEntity paymentTransactionEntity) throws ResponseException;
}
