package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class ProviderNameSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.PROVIDER__NAME and use the key directly
    public static final String KEY = SystemConfigKey.PROVIDER__NAME.getKey();

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.TEXT;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Name des Anbieters";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Name des Anbieters, der in der Anwendung angezeigt wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
