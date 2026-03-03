package de.aivot.GoverBackend.asset.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class DefaultStorageAssetsSystemConfigDefinition implements SystemConfigDefinition {
    public static final String DEFAULT_STORAGE_ASSETS_KEY = "storage.assets.default_storage_provider";

    @Nonnull
    @Override
    public String getKey() {
        return DEFAULT_STORAGE_ASSETS_KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.TEXT;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Speicher";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Standard Speicheranbieter für Assets";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Legt den Standard Speicheranbieter fest, der für das Speichern von Assets verwendet wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }
}
