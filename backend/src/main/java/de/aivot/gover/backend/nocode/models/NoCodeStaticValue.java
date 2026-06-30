package de.aivot.gover.backend.nocode.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import de.aivot.gover.backend.utils.StringUtils;

import java.util.Objects;

/**
 * Represents a static value in the NoCode language.
 */
public class NoCodeStaticValue extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeStaticValue";

    @Nullable
    private Object value;

    public NoCodeStaticValue() {
        super(TYPE_ID);
    }

    public NoCodeStaticValue(@Nullable Object value) {
        super(TYPE_ID);
        this.value = value;
    }

    public static NoCodeStaticValue of(@Nullable Object value) {
        return new NoCodeStaticValue(value);
    }

    @Nonnull
    @Override
    public NoCodeOperandError validate() {
        return NoCodeOperandError.NO_ERROR(this);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeStaticValue that = (NoCodeStaticValue) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(value);
        return result;
    }

    // endregion

    // region Getters & Setters

    public NoCodeStaticValue setValue(@Nullable Object value) {
        this.value = value;
        return this;
    }

    @Nullable
    public Object getValue() {
        if (value instanceof String sValue && StringUtils.isNullOrEmpty(sValue)) {
            return null;
        }
        return value;
    }

    // endregion
}
