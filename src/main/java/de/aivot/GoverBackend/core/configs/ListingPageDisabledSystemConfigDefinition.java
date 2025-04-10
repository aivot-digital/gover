package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class ListingPageDisabledSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.PROVIDER__LISTINGPAGE__DISABLE and use the key directly
    public static final String KEY = SystemConfigKey.PROVIDER__LISTINGPAGE__DISABLE.getKey();

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.FLAG;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Öffentliche Auflistung";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Öffentliche Auflistung deaktivieren";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Deaktiviert die öffentliche Auflistung der Formulare.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
