package de.aivot.GoverBackend.nocode.models;

import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Represents a reference to a field in a given values map which is passed into the NoCode evaluation process.
 */
public class NoCodeReference extends NoCodeOperand {
    public static final String TYPE_ID = "NoCodeReference";

    @Nullable
    private String elementId;

    public NoCodeReference() {
        super(TYPE_ID);
    }

    public NoCodeReference(@Nullable String elementId) {
        super(TYPE_ID);
        this.elementId = elementId;
    }

    public static NoCodeReference of(@Nullable String elementId) {
        return new NoCodeReference(elementId);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NoCodeReference that = (NoCodeReference) o;
        return Objects.equals(elementId, that.elementId);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(elementId);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getElementId() {
        return elementId;
    }

    public NoCodeReference setElementId(@Nullable String elementId) {
        this.elementId = elementId;
        return this;
    }

    // endregion
}
