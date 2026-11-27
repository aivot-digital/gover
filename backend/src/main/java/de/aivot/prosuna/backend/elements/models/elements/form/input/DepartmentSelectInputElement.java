package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/** Selects a department and persists only its stable database ID as the element value. */
public class DepartmentSelectInputElement extends BaseInputElement<Integer> implements PrintableElement<Integer> {
    @Nullable
    private String placeholder;

    @Nullable
    private String dialogTitle;

    public DepartmentSelectInputElement() {
        super(ElementType.DepartmentSelectInput);
    }

    @Nullable
    @Override
    public Integer formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer integer) {
            return integer;
        }

        if (value instanceof Number number) {
            var doubleValue = number.doubleValue();
            if (!Double.isFinite(doubleValue)
                    || doubleValue != Math.rint(doubleValue)
                    || doubleValue < Integer.MIN_VALUE
                    || doubleValue > Integer.MAX_VALUE) {
                return 0;
            }
            return (int) doubleValue;
        }

        if (value instanceof String stringValue) {
            var trimmedValue = stringValue.trim();
            if (trimmedValue.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(trimmedValue);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        return 0;
    }

    @Override
    public void performValidation(@Nullable Integer value) throws ValidationException {
        if (value != null && value <= 0) {
            throw new ValidationException(this, "Bitte wählen Sie eine gültige Organisationseinheit aus.");
        }
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable Integer value) {
        return value == null ? "Keine Angabe" : "Organisationseinheit (ID: %d)".formatted(value);
    }

    @Nonnull
    @Override
    public Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        var valueA = formatValue(referencedValue);
        var valueB = formatValue(comparedValue);

        return switch (operator) {
            case Equals -> Objects.equals(valueA, valueB);
            case NotEquals -> !Objects.equals(valueA, valueB);
            case Empty -> valueA == null;
            case NotEmpty -> valueA != null;
            default -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DepartmentSelectInputElement that = (DepartmentSelectInputElement) o;
        return Objects.equals(placeholder, that.placeholder) && Objects.equals(dialogTitle, that.dialogTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), placeholder, dialogTitle);
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DepartmentSelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Nullable
    public String getDialogTitle() {
        return dialogTitle;
    }

    public DepartmentSelectInputElement setDialogTitle(@Nullable String dialogTitle) {
        this.dialogTitle = dialogTitle;
        return this;
    }
}
