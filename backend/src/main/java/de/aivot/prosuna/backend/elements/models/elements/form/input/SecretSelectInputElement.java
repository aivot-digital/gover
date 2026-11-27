package de.aivot.prosuna.backend.elements.models.elements.form.input;

import de.aivot.prosuna.backend.elements.models.elements.BaseInputElement;
import de.aivot.prosuna.backend.elements.models.elements.PrintableElement;
import de.aivot.prosuna.backend.enums.ConditionOperator;
import de.aivot.prosuna.backend.enums.ElementType;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Selects a stored secret and persists only its key as the element value.
 */
public class SecretSelectInputElement extends BaseInputElement<String> implements PrintableElement<String> {
    @Nullable
    private String placeholder;

    public SecretSelectInputElement() {
        super(ElementType.SecretSelectInput);
    }

    @Nullable
    @Override
    public String formatValue(@Nullable Object value) {
        return StringUtils.toNullableTrimmedString(value);
    }

    @Override
    public void performValidation(@Nullable String value) throws ValidationException {
        // No additional validation is required. Availability is resolved through the secrets API in the editor.
    }

    @Nonnull
    @Override
    public String toDisplayValue(@Nullable String value) {
        return StringUtils.isNullOrEmpty(value) ? "Keine Angabe" : value;
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

        SecretSelectInputElement that = (SecretSelectInputElement) o;
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

    public SecretSelectInputElement setPlaceholder(@Nullable String placeholder) {
        this.placeholder = placeholder;
        return this;
    }
}
