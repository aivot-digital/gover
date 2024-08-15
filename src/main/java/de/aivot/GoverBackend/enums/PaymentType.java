package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum PaymentType implements Identifiable<String> {
    UpfrontFixed("upfrontFixed"),
    UpfrontCalculated("upfrontCalculated"),
    Downstream("downstream");

    private final String key;

    PaymentType(String key) {
        this.key = key;
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
}
