package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class GlobalThemeSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.SYSTEM__THEME and use the key directly
    public static final String KEY = SystemConfigKey.SYSTEM__THEME.getKey();

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.THEME;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Globales Erscheinungsbild";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Das globale Erscheinungsbild, das in der gesamten Anwendung verwendet wird. Es wird auch genutzt, wenn für ein Formular kein eigenes Erscheinungsbild definiert ist.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
