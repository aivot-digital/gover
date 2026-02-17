package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class CodeInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String code;

    public CodeInputElement() {
        super(ElementType.CodeInput);
    }

    @Override
    public String formatValue(Object value) {
        return switch (value) {
            case String sValue -> sValue;
            case null, default -> null;
        };
    }

    @Override
    public void performValidation(String value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public String toDisplayValue(@Nullable String value) {
        return value != null ? value : "Keine Angabe";
    }

    // region Hash & Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CodeInputElement that = (CodeInputElement) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), code);
    }

    // endregion

    // region Getters & Setters

    @Nullable
    public String getCode() {
        return code;
    }

    public CodeInputElement setCode(@Nullable String code) {
        this.code = code;
        return this;
    }

    // endregion
}
