package de.aivot.prosuna.backend.payment.models;

public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELED;

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isPaid() {
        return this == PAID;
    }

    public boolean isTerminal() {
        return this != PENDING;
    }
}
