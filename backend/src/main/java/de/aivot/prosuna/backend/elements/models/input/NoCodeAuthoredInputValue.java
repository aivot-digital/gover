package de.aivot.prosuna.backend.elements.models.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.aivot.prosuna.backend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nonnull;

public record NoCodeAuthoredInputValue(
        @JsonProperty("type") String type,
        @Nonnull NoCodeOperand operand
) implements AuthoredInputValue {
    public static final String TYPE = "NoCode";

    public NoCodeAuthoredInputValue {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException("Invalid type for a no-code authored input value.");
        }
        if (operand == null) {
            throw new IllegalArgumentException("A no-code authored input value requires an operand.");
        }
    }

    public NoCodeAuthoredInputValue(@Nonnull NoCodeOperand operand) {
        this(TYPE, operand);
    }
}
