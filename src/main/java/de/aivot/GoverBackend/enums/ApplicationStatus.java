package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum ApplicationStatus implements Identifiable<Integer> {
    Drafted(0),
    InReview(1),
    Published(2),
    Revoked(3);

    private final Integer key;

    private ApplicationStatus(Integer key) {
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
