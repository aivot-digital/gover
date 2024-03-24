package de.aivot.GoverBackend.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.GoverBackend.lib.Identifiable;

public enum EntityLockState implements Identifiable<Integer> {
    Free(0),
    LockedOther(1),
    LockedSelf(2);

    private final Integer key;

    EntityLockState(Integer key) {
        this.key = key;
    }

    @Override
    @JsonValue
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        return this.key.equals(other);
    }
}
