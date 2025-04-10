package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class LogoSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.SYSTEM__LOGO and use the key directly
    public static final String KEY = SystemConfigKey.SYSTEM__LOGO.getKey();

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.ASSET;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Logo";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Das Logo, welches in der Anwendung angezeigt wird.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
