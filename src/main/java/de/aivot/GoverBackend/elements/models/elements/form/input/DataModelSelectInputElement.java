package de.aivot.GoverBackend.elements.models.elements.form.input;

import de.aivot.GoverBackend.elements.models.elements.BaseInputElement;
import de.aivot.GoverBackend.elements.models.elements.PrintableElement;
import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class DataModelSelectInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String placeholder;

    public DataModelSelectInputElement() {
        super(ElementType.DataModelSelect);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var normalized = value.toString().trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        // No additional validation required.
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        if (StringUtils.isNullOrEmpty(value)) {
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
            case Empty -> StringUtils.isNullOrEmpty(valueA);
            case NotEmpty -> StringUtils.isNotNullOrEmpty(valueA);
            default -> false;
        };
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DataModelSelectInputElement that = (DataModelSelectInputElement) o;
        return Objects.equals(placeholder, that.placeholder);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Objects.hashCode(placeholder);
        return result;
    }

    @Nullable
    public String getPlaceholder() {
        return placeholder;
    }

    public DataModelSelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }
}
