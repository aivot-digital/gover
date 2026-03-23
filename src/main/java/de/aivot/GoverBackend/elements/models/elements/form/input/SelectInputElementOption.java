package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class SelectInputElementOption implements Serializable {
    @Nullable
    private String value;

    @Nullable
    private String label;

    @Nullable
    private String group;

    public static SelectInputElementOption of(String value, String label) {
        return new SelectInputElementOption()
                .setValue(value)
                .setLabel(label);
    }

    public static SelectInputElementOption of(String value, String label, @Nullable String group) {
        return of(value, label)
                .setGroup(group);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        SelectInputElementOption that = (SelectInputElementOption) o;
        return Objects.equals(value, that.value)
                && Objects.equals(label, that.label)
                && Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(label);
        result = 31 * result + Objects.hashCode(group);
        return result;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public SelectInputElementOption setValue(@Nullable String value) {
        this.value = value;
        return this;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public SelectInputElementOption setLabel(@Nullable String label) {
        this.label = label;
        return this;
    }

    @Nullable
    public String getGroup() {
        return group;
    }

    public SelectInputElementOption setGroup(@Nullable String group) {
        this.group = group;
        return this;
    }
}
