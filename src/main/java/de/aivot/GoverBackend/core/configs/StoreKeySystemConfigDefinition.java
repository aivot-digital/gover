package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class StoreKeySystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.GOVER__STORE_KEY and use the key directly
    public static final String KEY = SystemConfigKey.GOVER__STORE_KEY.getKey();

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.SECRET;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Gover Store";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Store-Schlüssel";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Schlüssel für den Zugriff auf den Gover Store.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }
}
