package de.aivot.prosuna.backend.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PhoneNumberValidation {
    Possible("possible"), Strict("strict");
    private final String value;
    PhoneNumberValidation(String value) { this.value = value; }
    @JsonValue public String getValue() { return value; }
}
