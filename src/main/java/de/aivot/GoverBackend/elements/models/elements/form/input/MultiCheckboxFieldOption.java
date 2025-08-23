package de.aivot.GoverBackend.elements.models.elements.form.input;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

public class MultiCheckboxFieldOption implements Serializable {
    @Nullable
    private String value;
    @Nullable
    private String label;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        MultiCheckboxFieldOption that = (MultiCheckboxFieldOption) o;
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

    public MultiCheckboxFieldOption setValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public MultiCheckboxFieldOption setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }
}
