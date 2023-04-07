package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;
import de.aivot.GoverBackend.models.elements.form.content.Alert;

import java.util.Arrays;
import java.util.Optional;

public enum AlertType implements Identifiable<Integer> {
    Error(0),
    Warning(1),
    Info(2),
    Success(3);

    private final Integer key;

    private AlertType(Integer key) {
        this.key = key;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return key.equals(other);
    }

    public static Optional<AlertType> findElement(Object id) {
        return Arrays
                .stream(AlertType.values())
                .filter(e -> e.matches(id))
                .findFirst();
    }
}
