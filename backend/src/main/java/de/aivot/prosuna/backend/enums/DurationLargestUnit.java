package de.aivot.prosuna.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DurationLargestUnit {
    Hour("hour"),
    Day("day");

    private final String value;

    DurationLargestUnit(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
