package de.aivot.prosuna.backend.config.models;

import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface SystemConfigDefinition<T> {
    @Nonnull
    String getKey();

    @Nonnull
    BaseElement getConfigElement();

    @Nonnull
    String getCategory();

    @Nullable
    default String getSubCategory() {
        return null;
    }

    @Nonnull
    String getLabel();

    @Nonnull
    String getDescription();

    @Nonnull
    default Boolean isPublicConfig() {
        return false;
    }

    @Nullable
    default T getDefaultValue() {
        return null;
    }

    @Nonnull
    default String serializeValueToDB(@Nullable T value) throws ResponseException {
        return value == null ? "" : value.toString();
    }

    @Nullable
    T parseValueFromDB(@Nonnull String value) throws ResponseException;

    default void validate(@Nullable T value) throws ResponseException {
        // Raise an exception here if the value is broken
    }

    default void validateChange(@Nullable T oldValue,
                                @Nullable T newValue,
                                boolean changeConfirmed) throws ResponseException {
        // Raise an exception here if changing from oldValue to newValue needs extra validation
    }
}
