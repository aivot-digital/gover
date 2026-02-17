package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class MultiCheckboxInputElementOption implements Serializable {
    @Nullable
    private String value;

    @Nullable
    private String label;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MultiCheckboxInputElementOption that = (MultiCheckboxInputElementOption) o;
        return Objects.equals(value, that.value) && Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(label);
        return result;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public MultiCheckboxInputElementOption setValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public MultiCheckboxInputElementOption setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }
}
