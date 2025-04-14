package de.aivot.GoverBackend.identity.enums;

import de.aivot.GoverBackend.lib.models.Identifiable;

public enum IdentityOriginState implements Identifiable<Integer> {
    Success(0),
    UnknownError(500);

    private final int key;

    IdentityOriginState(int key) {
        this.key = key;
    }

    @Override
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        if (other instanceof IdentityOriginState) {
            return this.key == ((IdentityOriginState) other).key;
        }
        if (other instanceof Integer) {
            return this.key == (Integer) other;
        }
        return false;
    }
}
