package de.aivot.prosuna.backend.identity.enums;

import de.aivot.prosuna.backend.lib.models.Identifiable;

public enum IdentityResultState implements Identifiable<Integer> {
    Success(0),
    UnknownError(500);

    private final int key;

    IdentityResultState(int key) {
        this.key = key;
    }

    @Override
    public Integer getKey() {
        return key;
    }

    @Override
    public boolean matches(Object other) {
        if (other instanceof IdentityResultState) {
            return this.key == ((IdentityResultState) other).key;
        }
        if (other instanceof Integer) {
            return this.key == (Integer) other;
        }
        return false;
    }
}
