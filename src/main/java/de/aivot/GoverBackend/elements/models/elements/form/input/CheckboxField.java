package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.RequiredValidationException;
import de.aivot.GoverBackend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CheckboxField extends BaseInputElement<Boolean> {
    public CheckboxField() {
        super(ElementType.Checkbox);
    }

    @Nullable
    @Override
    public Boolean formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Boolean value) {
        return value != null && value ? "Ja" : "Nein";
    }

    @Override
    public void performValidation(@Nullable Boolean value) throws ValidationException {
        if (Boolean.TRUE.equals(getRequired()) && !Boolean.TRUE.equals(value)) {
            throw new RequiredValidationException(this);
        }
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        Boolean valA = formatValue(referencedValue);
        Boolean valB = formatValue(comparedValue);

        return switch (operator) {
            case Equals -> valA == valB;
            case NotEquals -> valA != valB;

            case Empty -> !Boolean.TRUE.equals(valA);
            case NotEmpty -> Boolean.TRUE.equals(valA);

            default -> false;
        };
    }

    public static Boolean _formatValue(@Nullable Object value) {
        return switch (value) {
            case null -> false;
            case String sValue -> "Ja (True)".equalsIgnoreCase(sValue) ||
                                  "Ja".equalsIgnoreCase(sValue) ||
                                  "true".equalsIgnoreCase(sValue) ||
                                  "wahr".equalsIgnoreCase(sValue) ||
                                  "1".equals(sValue);
            case Boolean bValue -> bValue;
            default -> false;
        };

    }
}
