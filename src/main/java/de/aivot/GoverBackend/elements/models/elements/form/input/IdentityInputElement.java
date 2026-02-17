package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class IdentityInputElement extends BaseInputElement<IdentityInputElementItem> implements PrintableElement<IdentityInputElementItem> {
    @Nullable
    private List<IdentityInputElementOption> options;

    @Nullable
    private Boolean allowsMail;

    public IdentityInputElement() {
        super(ElementType.IdentityInput);
    }

    @Override
    public IdentityInputElementItem formatValue(Object value) {
        if (value == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(value, IdentityInputElementItem.class);
    }

    @Override
    public void performValidation(IdentityInputElementItem value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public String toDisplayValue(@Nullable IdentityInputElementItem value) {
        return value != null ? value.getIdentityData().toString() : "Keine Angabe";
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IdentityInputElement that = (IdentityInputElement) o;
        return Objects.equals(options, that.options) && Objects.equals(allowsMail, that.allowsMail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), options, allowsMail);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public List<IdentityInputElementOption> getOptions() {
        return options;
    }

    public IdentityInputElement setOptions(@Nullable List<IdentityInputElementOption> options) {
        this.options = options;
        return this;
    }

    @Nullable
    public Boolean getAllowsMail() {
        return allowsMail;
    }

    public IdentityInputElement setAllowsMail(@Nullable Boolean allowsMail) {
        this.allowsMail = allowsMail;
        return this;
    }

    // endregion
}
