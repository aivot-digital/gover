package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.nocode.models.NoCodeOperand;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class NoCodeInputElementItem implements Serializable {
    @Nullable
    private NoCodeOperand noCode;

    // region constructors

    // Empty constructor for serialization
    public NoCodeInputElementItem() {
    }

    // Full constructor
    public NoCodeInputElementItem(@Nullable NoCodeOperand noCode) {
        this.noCode = noCode;
    }

    // endregion

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        NoCodeInputElementItem that = (NoCodeInputElementItem) o;
        return Objects.equals(noCode, that.noCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(noCode);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public NoCodeOperand getNoCode() {
        return noCode;
    }

    public NoCodeInputElementItem setNoCode(@Nullable NoCodeOperand noCode) {
        this.noCode = noCode;
        return this;
    }

    // endregion
}
