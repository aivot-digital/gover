package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
public class FaviconSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.SYSTEM__FAVICON and use the key directly
    public static final String KEY = SystemConfigKey.SYSTEM__FAVICON.getKey();

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
        return "Browser-Tab-Icon";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Das Icon, welches im Browser-Tab angezeigt wird.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }

    @Nullable
    @Override
    public Object getDefaultValue() {
        return "";
    }
}
