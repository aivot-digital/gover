package de.aivot.prosuna.backend.elements.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import de.aivot.prosuna.backend.lib.models.Identifiable;

public enum OptionsSourceType implements Identifiable<String> {
    Manual("manual"),
    CodeList("code_list");

    private final String key;

    OptionsSourceType(String key) {
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
    public static OptionsSourceType fromKey(String key) {
        for (var value : values()) {
            if (value.matches(key)) {
                return value;
            }
        }
        return Manual;
    }
}
