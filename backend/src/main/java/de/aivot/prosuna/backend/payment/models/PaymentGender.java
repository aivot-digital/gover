package de.aivot.prosuna.backend.payment.models;

public enum PaymentGender {
    M,
    F,
    D;

    public boolean matches(String value) {
        return name().equalsIgnoreCase(value);
    }
}
