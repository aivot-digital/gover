package de.aivot.prosuna.backend.elements.models.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record LiteralAuthoredInputValue(
        @JsonProperty("type") String type,
        @Nullable Object value
) implements AuthoredInputValue {
    public static final String TYPE = "Literal";

    public LiteralAuthoredInputValue {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid type for a literal authored input value.");
        }
    }

    public LiteralAuthoredInputValue(@Nullable Object value) {
        this(TYPE, value);
    }
}
