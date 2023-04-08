package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;
import de.aivot.GoverBackend.models.elements.form.content.Alert;

import java.util.Arrays;
import java.util.Optional;

public enum AlertType implements Identifiable<String> {
    Error("error"),
    Warning("warning"),
    Info("info"),
    Success("success");

    private final String key;

    private AlertType(String key) {
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
