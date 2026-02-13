package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class LogoSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.SYSTEM__LOGO and use the key directly
    public static final String KEY = SystemConfigKey.SYSTEM__LOGO.getKey();

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.ASSET;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Logo";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Das Logo, welches in der Anwendung angezeigt wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
