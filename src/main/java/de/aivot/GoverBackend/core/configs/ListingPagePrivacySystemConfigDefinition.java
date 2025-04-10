package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class ListingPagePrivacySystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.PROVIDER__LISTINGPAGE__PRIVACYDEPARTMENTID and use the key directly
    public static final String KEY = SystemConfigKey.PROVIDER__LISTINGPAGE__PRIVACYDEPARTMENTID.getKey();

    @NotNull
    @Override
    public String getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public ConfigType getType() {
        return ConfigType.DEPARTMENT;
    }

    @NotNull
    @Override
    public String getCategory() {
        return "Öffentliche Auflistung";
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Datenschutzerklärung";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Der für den Datenschutz zuständige Fachbereich.";
    }

    @NotNull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
