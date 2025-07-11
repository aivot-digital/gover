package de.aivot.GoverBackend.elements.models.elements.form.input;

import javax.annotation.Nullable;
import java.util.Objects;

public class RadioFieldOption {
    @Nullable
    private String value;
    @Nullable
    private String label;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        RadioFieldOption that = (RadioFieldOption) o;
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

    public RadioFieldOption setValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public RadioFieldOption setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }
}
