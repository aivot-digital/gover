package de.aivot.GoverBackend.elements.models.elements;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface PrintableElement<T> {
    @Nonnull
    default String display(Object value) {
        var formattedValue = formatValue(value);
        return toDisplayValue(formattedValue);
    }

    @Nullable
    T formatValue(@Nullable Object value);

    @Nonnull
    String toDisplayValue(@Nullable T value);
}
