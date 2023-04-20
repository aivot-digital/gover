package de.aivot.GoverBackend.models.elements.form;

import de.aivot.GoverBackend.models.elements.BaseElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public abstract class BaseFormElement extends BaseElement {
    private Integer weight;

    protected BaseFormElement(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        weight = MapUtils.getInteger(values, "weight");
    }

    //region Getters & Setters

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    //endregion
}
