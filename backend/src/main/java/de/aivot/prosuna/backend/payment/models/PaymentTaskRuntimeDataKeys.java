package de.aivot.prosuna.backend.payment.models;

/**
 * Runtime data keys used by process tasks that wait for a payment transaction.
 */
public final class PaymentTaskRuntimeDataKeys {
    public static final String PAYMENT_PAYLOAD = "paymentPayload";
    public static final String PAYMENT_TRANSACTION_KEY = "paymentTransaction";

    private PaymentTaskRuntimeDataKeys() {
    }
}
