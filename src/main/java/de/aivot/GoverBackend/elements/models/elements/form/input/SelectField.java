package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.utils.MapUtils;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

public class SelectField extends RadioField {
    @Nullable
    private String placeholder;
    @Nullable
    private String autocomplete;

    public SelectField() {
        super();
        setType(ElementType.Select);
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SelectField that = (SelectField) o;
        return Objects.equals(placeholder, that.placeholder) && Objects.equals(autocomplete, that.autocomplete);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        result = 31 * result + Objects.hashCode(autocomplete);
        return result;
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public SelectField setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getAutocomplete() {
        return autocomplete;
    }

    public SelectField setAutocomplete(@Nullable String autocomplete) {
        this.autocomplete = autocomplete;
        return this;
    }

    // endregion
}
