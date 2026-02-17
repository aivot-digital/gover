package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.data.SystemConfigKey;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

@Component
public class ListingPageCustomLinkSystemConfigDefinition implements SystemConfigDefinition {
    // TODO: Remove SystemConfigKey.PROVIDER__LISTINGPAGE__CUSTOMLINK and use the key directly
    public static final String KEY = SystemConfigKey.PROVIDER__LISTINGPAGE__CUSTOMLINK.getKey();

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
        return "Öffentliche Auflistung";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Externer Formular-Index-Link";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Link zu einer externen Übersichtsseite der Formulare.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }
}
