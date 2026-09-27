package de.aivot.prosuna.backend.elements.models.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;

public record LowCodeAuthoredInputValue(
        @JsonProperty("type") String type,
        @Nonnull String code
) implements AuthoredInputValue {
    public static final String TYPE = "LowCode";

    public LowCodeAuthoredInputValue {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid type for a low-code authored input value.");
        }
        if (code == null) {
            throw new IllegalArgumentException("A low-code authored input value requires code.");
        }
    }

    public LowCodeAuthoredInputValue(@Nonnull String code) {
        this(TYPE, code);
    }
}
