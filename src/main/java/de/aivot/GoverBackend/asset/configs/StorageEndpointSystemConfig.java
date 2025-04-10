package de.aivot.GoverBackend.asset.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

@Component
public class StorageEndpointSystemConfig implements SystemConfigDefinition {
    public static final String KEY = "storage.endpoint";

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
        return "Speicher";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "S3-Speicher-Endpunkt";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Endpunkt des S3-Speichers.";
    }
}
