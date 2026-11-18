package de.aivot.prosuna.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DurationPrecision {
    Minute("minute"),
    Second("second");

    private final String value;

    DurationPrecision(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
