package de.aivot.gover.backend.payment.exceptions;

public class PaymentException extends Exception {
    public PaymentException(String message, Object ...args) {
        super(String.format(message, args));
    }

    public PaymentException(Throwable cause, String message, Object ...args) {
        super(String.format(message, args), cause);
    }
}
