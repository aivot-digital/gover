package de.aivot.prosuna.backend.elements.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.prosuna.backend.lib.models.Identifiable;

public enum AssetVisibility implements Identifiable<String> {
    All("all"),
    Public("public"),
    Private("private");

    private final String key;

    AssetVisibility(String key) {
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

    @JsonCreator
    public static AssetVisibility fromKey(String key) {
        if (key == null) {
            return All;
        }

        for (var value : values()) {
            if (value.matches(key)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown asset visibility: " + key);
    }
}
