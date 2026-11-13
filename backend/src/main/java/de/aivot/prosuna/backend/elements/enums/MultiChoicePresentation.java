package de.aivot.prosuna.backend.elements.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public enum MultiChoicePresentation {
    Checkboxes("checkboxes"),
    Combobox("combobox");

    private final String key;

    MultiChoicePresentation(@Nonnull String key) {
        this.key = key;
    }

    @JsonValue
    @Nonnull
    public String getKey() {
        return key;
    }

    @JsonCreator
    @Nonnull
    public static MultiChoicePresentation fromKey(@Nullable String key) {
        for (var value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        return Checkboxes;
    }
}
