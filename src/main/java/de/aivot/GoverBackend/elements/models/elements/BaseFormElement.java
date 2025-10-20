package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.enums.ElementType;

import java.util.Objects;

public abstract class BaseFormElement extends BaseElement {
    private Double weight;

    public BaseFormElement(ElementType type) {
        super(type);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseFormElement that = (BaseFormElement) o;
        return Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(weight);
        return result;
    }

    // endregion

    // region Getters & Setters

    public Double getWeight() {
        return weight;
    }

    public BaseFormElement setWeight(Double weight) {
        this.weight = weight;
        return this;
    }

    // endregion
}
