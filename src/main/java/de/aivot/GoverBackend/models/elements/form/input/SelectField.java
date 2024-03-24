package de.aivot.GoverBackend.models.elements.form.input;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SelectField that = (SelectField) o;

        return Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (placeholder != null ? placeholder.hashCode() : 0);
        return result;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

}
