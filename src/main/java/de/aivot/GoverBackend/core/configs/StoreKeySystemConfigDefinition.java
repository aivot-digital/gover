package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class StoreKeySystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.GOVER__STORE_KEY and use the key directly
    public static final String KEY = SystemConfigKey.GOVER__STORE_KEY.getKey();

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.SECRET;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Gover Store";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Store-Schlüssel";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Der Schlüssel für den Zugriff auf den Gover Store.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }
}
