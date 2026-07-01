package de.aivot.gover.backend.preset.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.gover.backend.lib.models.Identifiable;


public enum PresetStatus implements Identifiable<Integer> {
    Drafted(0),
    Published(1),
    Revoked(2);

    private final Integer key;

    PresetStatus(Integer key) {
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
