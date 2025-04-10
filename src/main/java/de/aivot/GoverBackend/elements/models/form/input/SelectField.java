package de.aivot.GoverBackend.elements.models.form.input;

import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class SelectField extends RadioField {
    private String placeholder;
    private String autocomplete;

    public SelectField(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);

        placeholder = MapUtils.getString(values, "placeholder");
        autocomplete = MapUtils.getString(values, "autocomplete");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SelectField that = (SelectField) o;

        if (!Objects.equals(placeholder, that.placeholder))
            return false;
        return Objects.equals(autocomplete, that.autocomplete);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (placeholder != null ? placeholder.hashCode() : 0);
        result = 31 * result + (autocomplete != null ? autocomplete.hashCode() : 0);
        return result;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getAutocomplete() {
        return autocomplete;
    }

    public void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

}
