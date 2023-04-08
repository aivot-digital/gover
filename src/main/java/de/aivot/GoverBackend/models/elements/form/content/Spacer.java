package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.form.FormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class Spacer extends FormElement {
    private Integer height;

    public Spacer(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        height = MapUtils.getInteger(values, "height", 100);
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
