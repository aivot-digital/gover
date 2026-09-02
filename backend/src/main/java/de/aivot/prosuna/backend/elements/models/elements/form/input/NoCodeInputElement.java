package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.core.services.JsonMapperFactory;
import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.RequiredValidationException;
import de.aivot.prosuna.backend.exceptions.ValidationException;
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

        return JsonMapperFactory
                .getInstance()
                .convertValue(value, NoCodeInputElementItem.class);
    }

    @Override
    public void performValidation(@Nullable NoCodeInputElementItem value) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !isFilled(value)) {
            throw new RequiredValidationException(this);
        }

        if (value != null) {
            var noCode = value.getNoCode();
            if (noCode != null) {
                var noCodeError = noCode.validate();
                if (!noCodeError.isValid()) {
                    throw new ValidationException(
                            this,
                            "Der No-Code-Ausdruck ist ungültig.",
                            noCodeError
                    );
                }
            }
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
        RUNTIME,
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        DATETIME,
        TIME
    }
}
