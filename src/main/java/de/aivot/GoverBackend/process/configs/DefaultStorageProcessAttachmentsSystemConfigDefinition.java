package de.aivot.GoverBackend.process.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DefaultStorageProcessAttachmentsSystemConfigDefinition implements SystemConfigDefinition {
    public static final String KEY = "storage.attachments.default_storage_provider";

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.TEXT;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Speicher";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Standard Speicheranbieter für Anhängen von Vorgängen";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Legt den Standard Speicheranbieter fest, der für das Speichern von Anhängen von Vorgängen verwendet wird.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }
}
