package de.aivot.GoverBackend.models.elements.form;

import de.aivot.GoverBackend.models.elements.BaseElement;
import org.springframework.lang.Nullable;

import java.util.Map;

public abstract class FormElement extends BaseElement {
    private Integer weight;

    protected FormElement(Map<String, Object> data) {
        super(data);
        weight = (Integer) data.get("weight");
    }

    @Nullable
    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }
}
