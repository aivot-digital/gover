package de.aivot.GoverBackend.models.elements.form;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.nio.DoubleBuffer;
import java.util.Map;
import java.util.Objects;

public abstract class BaseFormElement extends BaseElement {
    private Double weight;

    protected BaseFormElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        weight = MapUtils.getDouble(values, "weight");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        BaseFormElement that = (BaseFormElement) o;

        return Objects.equals(weight, that.weight);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (weight != null ? weight.hashCode() : 0);
        return result;
    }

    //region Getters & Setters

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    //endregion
}
