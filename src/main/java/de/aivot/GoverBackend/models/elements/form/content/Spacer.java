package de.aivot.GoverBackend.models.elements.form.content;

import de.aivot.GoverBackend.models.elements.form.BaseFormElement;
import de.aivot.GoverBackend.utils.MapUtils;

import java.util.Map;
import java.util.Objects;

public class Spacer extends BaseFormElement {
    private String height;

    public Spacer(Map<String, Object> data) {
        super(data);
    }

    @Override
    public void applyValues(Map<String, Object> values) {
        super.applyValues(values);
        height = MapUtils.getString(values, "height", "100");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Spacer spacer = (Spacer) o;

        return Objects.equals(height, spacer.height);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (height != null ? height.hashCode() : 0);
        return result;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        this.height = height;
    }
}
