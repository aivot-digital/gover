package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class GlobalThemeSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.SYSTEM__THEME and use the key directly
    public static final String KEY = SystemConfigKey.SYSTEM__THEME.getKey();

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.THEME;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Globales Farbschema";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Das globale Farbschema, welches auf der gesamten Seite angewendet wird. Dieses wird auch verwendet, wenn kein Farbschema für ein Formular definiert ist.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
