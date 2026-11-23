package de.aivot.prosuna.backend.nocode.models;

import de.aivot.prosuna.backend.nocode.exceptions.NoCodeException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represents the result of a NoCode evaluation.
 * Contains the value of the result.
 */
public class NoCodeResult {
    private final Object value;

    public NoCodeResult(@Nullable Object value) {
        this.value = value;
    }

    /**
     * Returns whether the result is null.
     *
     * @return whether the result is null
     */
    @Nonnull
    public Boolean isNull() {
        return value == null;
    }

    /**
     * Returns the value of the result as an object.
     *
     * @return the value of the result as an object.
     */
    @Nullable
    public Object getValue() {
        return value;
    }

    /**
     * Returns the value of the result as a condition, read like the conditions of operators.
     *
     * @throws IllegalStateException if the value is neither true nor false, with a message for the people who author
     *                               the expression.
     */
    @Nonnull
    public Boolean getValueAsBoolean() {
        try {
            return NoCodeOperator.castToBoolean(isNull() ? null : value);
        } catch (NoCodeException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
