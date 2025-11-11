package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.nocode.models.NoCodeOperand;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

public class ValidationNoCodeWrapper implements Serializable {
    @Nullable
    private NoCodeOperand noCode;
    @Nullable
    private String message;

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        ValidationNoCodeWrapper that = (ValidationNoCodeWrapper) o;
        return Objects.equals(noCode, that.noCode) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(noCode);
        result = 31 * result + Objects.hashCode(message);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public NoCodeOperand getNoCode() {
        return noCode;
    }

    public ValidationNoCodeWrapper setNoCode(@Nullable NoCodeOperand noCode) {
        this.noCode = noCode;
        return this;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    public ValidationNoCodeWrapper setMessage(@Nullable String message) {
        this.message = message;
        return this;
    }

    // endregion
}
