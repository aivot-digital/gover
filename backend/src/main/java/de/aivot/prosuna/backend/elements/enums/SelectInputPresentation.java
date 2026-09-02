package de.aivot.prosuna.backend.elements.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.prosuna.backend.lib.models.Identifiable;

public enum SelectInputPresentation implements Identifiable<String> {
    Dropdown("dropdown"),
    Combobox("combobox");

    private final String key;

    SelectInputPresentation(String key) {
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
    public static SelectInputPresentation fromKey(String key) {
        for (var value : values()) {
            if (value.matches(key)) {
                return value;
            }
        }

        return Dropdown;
    }
}
