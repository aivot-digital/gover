package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Component
public class ExperimentalFeaturesNewCodeEditorsSystemConfigDefinition implements SystemConfigDefinition {
    public static final String KEY = "ExperimentalFeaturesNewCodeEditors";

    @Value("${GOVER_FEATURES_NEW_CODE_EDITORS:false}")
    private String featureFlagValue;

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.FLAG;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Feature Flag: Neue Code-Editoren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Aktiviert die Vorschau-Version der neuen Code-Editoren.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }

    @Nonnull
    @Override
    public Boolean getDefaultValue() {
        return Boolean.parseBoolean(featureFlagValue);
    }

    @Nonnull
    @Override
    public String serializeValueToDB(@Nullable Object value) throws ResponseException {
        return value == null ? "false" : value.toString();
    }

    @Nullable
    @Override
    public Object parseValueFromDB(@Nonnull String value) throws ResponseException {
        return Boolean.parseBoolean(value);
    }
}
