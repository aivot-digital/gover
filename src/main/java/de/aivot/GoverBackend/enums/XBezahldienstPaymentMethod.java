package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum XBezahldienstPaymentMethod implements Identifiable<String> {
    GIROPAY("GIROPAY", "Giropay"),
    PAYDIRECT("PAYDIRECT", "PayDirect"),
    CREDITCARD("CREDITCARD", "Kreditkarte"),
    PAYPAL("PAYPAL", "PayPal"),
    OTHER("OTHER", "Anderes Zahlungsmittel");

    private final String key;
    private final String label;

    XBezahldienstPaymentMethod(String key, String label) {
        this.key = key;
        this.label = label;
    }

    @Override
    @JsonValue
    public String getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }

    public String getLabel() {
        return label;
    }
}
