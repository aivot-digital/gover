package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class NoCodeInputElement extends BaseInputElement<NoCodeInputElementItem> implements PrintableElement<NoCodeInputElementItem> {
    @Nonnull
    private NoCodeInputReturnType returnType = NoCodeInputReturnType.BOOLEAN;

    public NoCodeInputElement() {
        super(ElementType.NoCodeInput);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NoCodeInputElement that = (NoCodeInputElement) o;
        return returnType == that.returnType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), returnType);
    }

    @Override
    public NoCodeInputElementItem formatValue(Object value) {
        if (value == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(value, NoCodeInputElementItem.class);
    }

    @Override
    public void performValidation(@Nullable NoCodeInputElementItem value) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !isFilled(value)) {
            throw new RequiredValidationException(this);
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable NoCodeInputElementItem value) {
        if (!isFilled(value)) {
            return "Keine Angabe";
        }

        return "No-Code-Ausdruck konfiguriert";
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = formatValue(referencedValue);
        var isEmpty = !isFilled(valueA);

        return switch (operator) {
            case Empty -> isEmpty;
            case NotEmpty -> !isEmpty;
            default -> false;
        };
    }

    private boolean isFilled(@Nullable NoCodeInputElementItem value) {
        return value != null && value.getNoCode() != null;
    }

    @Nonnull
    public NoCodeInputReturnType getReturnType() {
        return returnType;
    }

    public NoCodeInputElement setReturnType(NoCodeInputReturnType returnType) {
        this.returnType = Objects.requireNonNullElse(returnType, NoCodeInputReturnType.BOOLEAN);
        return this;
    }

    public enum NoCodeInputReturnType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        DATETIME
    }
}
