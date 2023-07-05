package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;

public class SelectField extends RadioField {
    private String placeholder;

    public SelectField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

}
