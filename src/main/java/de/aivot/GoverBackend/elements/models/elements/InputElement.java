package de.aivot.GoverBackend.elements.models.elements;

import de.aivot.GoverBackend.enums.ConditionOperator;
import de.aivot.GoverBackend.enums.ElementType;
import de.aivot.GoverBackend.exceptions.ValidationException;
import de.aivot.GoverBackend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Objects;

public interface InputElement<T> {
    default void validate(@Nullable Object rawValue) throws ValidationException {
        if (rawValue == null) {
            if (Boolean.TRUE.equals(getRequired())) {
                throw new ValidationException(null, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
            }
        } else {
            T formattedValue = formatValue(rawValue);

            if (Boolean.TRUE.equals(getRequired())) {
                if (formattedValue == null) {
                    throw new ValidationException(null, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
                if (formattedValue instanceof String && StringUtils.isNullOrEmpty((String) formattedValue)) {
                    throw new ValidationException(null, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
                if (formattedValue.getClass().isArray() && Array.getLength(formattedValue) == 0) {
                    throw new ValidationException(null, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
                if (formattedValue instanceof Collection<?> && ((Collection<?>) formattedValue).isEmpty()) {
                    throw new ValidationException(null, "Dieses Feld ist ein Pflichtfeld und darf nicht leer sein.");
                }
            }

            performValidation(formattedValue);
        }
    }
    @Nonnull
    String getId();

    Boolean getRequired();

    void performValidation(@Nullable T value) throws ValidationException;

    @Nonnull
    default Boolean evaluate(ConditionOperator operator, Object referencedValue, Object comparedValue) {
        return true;
    }

    @Nullable
    T formatValue(@Nullable Object value);

    @Nullable
    ElementValueFunctions getValue();

    @Nullable
    ElementValidationFunctions getValidation();
}
