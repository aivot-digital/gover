package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum XBezahldienstGender implements Identifiable<String> {
    MALE("M"),
    FEMALE("F"),
    DIVERSE("D");

    private final String key;

    XBezahldienstGender(String key) {
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
