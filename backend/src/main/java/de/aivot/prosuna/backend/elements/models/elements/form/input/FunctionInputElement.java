package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

        return JsonMapperFactory
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
