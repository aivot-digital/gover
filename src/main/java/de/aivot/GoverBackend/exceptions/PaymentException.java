package de.aivot.GoverBackend.exceptions;

public class PaymentException extends Exception {
    public PaymentException(String message, Object ...args) {
        super(String.format(message, args));
    }
}
