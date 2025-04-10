package de.aivot.GoverBackend.nocode.models;

import java.util.Objects;

/**
 * Represents a reference to a field in a given values map which is passed into the NoCode evaluation process.
 */
public class NoCodeReference implements NoCodeOperand {
    private final String elementId;

    public NoCodeReference(String elementId) {
        this.elementId = elementId;
    }

    /**
     * Returns the id of the element that is referenced.
     *
     * @return the id of the element that is referenced
     */
    public String getElementId() {
        return elementId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NoCodeReference that = (NoCodeReference) o;
        return Objects.equals(elementId, that.elementId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(elementId);
    }
}
