package de.aivot.GoverBackend.config.models;

import de.aivot.GoverBackend.config.entities.UserConfigEntity;
import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface UserConfigDefinition {
    @Nonnull
    String getKey();

    @Nonnull
    ConfigType getType();

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
    default List<ConfigDefinitionOption> getOptions() {
        return null;
    }

    @Nullable
    default Object getDefaultValue() {
        return null;
    }

    @Nonnull
    default String serializeValueToDB(@Nullable Object value) throws ResponseException {
        return value == null ? "" : value.toString();
    }

    @Nullable
    default Object parseValueFromDB(@Nonnull String value) throws ResponseException {
        return value;
    }

    default void validate(@Nullable UserConfigEntity value) throws ResponseException {
    }

    default void beforeChange(@Nonnull UserConfigEntity entity) throws ResponseException {
    }

    default void afterChange(@Nonnull UserConfigEntity entity) throws ResponseException {
    }

    default int getCategoryOrder() {
        return 100;
    }

    default int getSubCategoryOrder() {
        return 100;
    }

    default int getDefinitionOrder() {
        return 100;
    }

}
