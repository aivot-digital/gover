package de.aivot.GoverBackend.nocode.models;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a static value in the NoCode language.
 */
public class NoCodeStaticValue implements NoCodeOperand {
    private final Object value;

    public NoCodeStaticValue(@Nullable Object value) {
        this.value = value;
    }

    /**
     * Returns the value of the static value.
     *
     * @return the value of the static value
     */
    @Nullable
    public Object getValue() {
        if (value instanceof String sValue && sValue.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NoCodeStaticValue that = (NoCodeStaticValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
