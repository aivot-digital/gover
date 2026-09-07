package de.aivot.prosuna.backend.elements.models.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;

public record VariableAuthoredInputValue(
        @JsonProperty("type") String type,
        @Nonnull InputVariableReference reference
) implements AuthoredInputValue {
    public static final String TYPE = "Variable";

    public VariableAuthoredInputValue {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid type for a variable authored input value.");
        }
        if (reference == null) {
            throw new IllegalArgumentException("A variable authored input value requires a reference.");
        }
    }

    public VariableAuthoredInputValue(@Nonnull InputVariableReference reference) {
        this(TYPE, reference);
    }
}
