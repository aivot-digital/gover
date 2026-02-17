package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.core.services.ObjectMapperFactory;
import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class FunctionInputElement extends BaseInputElement<FunctionInputElementItem> {
    @Nonnull
    private FunctionReturnType returnType = FunctionReturnType.BOOLEAN;

    public FunctionInputElement() {
        super(ElementType.FunctionInput);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FunctionInputElement that = (FunctionInputElement) o;
        return returnType == that.returnType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), returnType);
    }

    @Override
    public FunctionInputElementItem formatValue(Object value) {
        if (value == null) {
            return null;
        }

        return ObjectMapperFactory
                .getInstance()
                .convertValue(value, FunctionInputElementItem.class);
    }

    @Override
    public void performValidation(@Nullable FunctionInputElementItem value) throws ValidationException {
        if (value == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new RequiredValidationException(this);
            }
        }
    }

    @Nonnull
    public FunctionReturnType getReturnType() {
        return returnType;
    }

    public FunctionInputElement setReturnType(FunctionReturnType returnType) {
        this.returnType = Objects.requireNonNullElse(returnType, FunctionReturnType.BOOLEAN);
        return this;
    }

    public enum FunctionReturnType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        DATETIME
    }
}
