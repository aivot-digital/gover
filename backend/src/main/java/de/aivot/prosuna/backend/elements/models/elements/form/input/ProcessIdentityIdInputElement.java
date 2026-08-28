package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class ProcessIdentityIdInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String placeholder;

    public ProcessIdentityIdInputElement() {
        super(ElementType.ProcessIdentityIdInput);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        return _formatValue(value);
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return "Keine Angabe";
        }

        return value;
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

    @Nullable
    public static String _formatValue(@Nullable Object value) {
        if (!(value instanceof String stringValue)) {
            return null;
        }

        var normalizedValue = stringValue.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProcessIdentityIdInputElement that = (ProcessIdentityIdInputElement) o;
        return Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), placeholder);
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public ProcessIdentityIdInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }
}
