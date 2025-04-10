package de.aivot.GoverBackend.form.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.models.Identifiable;

public enum FormStatus implements Identifiable<Integer> {
    Drafted(0),
    InReview(1),
    Published(2),
    Revoked(3);

    private final Integer key;

    FormStatus(Integer key) {
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
