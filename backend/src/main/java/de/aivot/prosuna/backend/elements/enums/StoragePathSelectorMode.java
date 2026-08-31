package de.aivot.prosuna.backend.elements.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.prosuna.backend.lib.models.Identifiable;

public enum StoragePathSelectorMode implements Identifiable<String> {
    Folder("folder"),
    File("file");

    private final String key;

    StoragePathSelectorMode(String key) {
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
    public static StoragePathSelectorMode fromKey(String key) {
        if (key == null) {
            return Folder;
        }

        for (var value : values()) {
            if (value.matches(key)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown storage path selector mode: " + key);
    }
}
