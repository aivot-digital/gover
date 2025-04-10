package de.aivot.GoverBackend.form.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

public enum FormType implements Identifiable<Integer> {
    Public(0),
    Internal(1);

    private final Integer key;

    FormType(Integer key) {
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
}
