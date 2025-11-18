package de.aivot.GoverBackend.department.configs;

import de.aivot.GoverBackend.config.enums.ConfigType;
import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.utils.StringUtils;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Component
public class OrganizationalUnitLabelConfig implements SystemConfigDefinition {
    public static final String KEY = "org_unit.labels";

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public ConfigType getType() {
        return ConfigType.LIST;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Organisationseinheiten";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Benennung der Organisationseinheiten";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Definiert die Benennung der Organisationseinheiten. Zum Beispiel 'Abteilung', 'Team' oder 'Gruppe'.";
    }

    @Nullable
    @Override
    public Object parseValueFromDB(@Nonnull String value) throws ResponseException {
        if (StringUtils.isNullOrEmpty(value)) {
            return new String[]{};
        }
        return value.split("\\|");
    }

    @Nonnull
    @Override
    public String serializeValueToDB(@Nullable Object value) throws ResponseException {
        if (value == null) {
            return "";
        }
        if (!(value instanceof String[] labels)) {
            throw ResponseException.badRequest("Ungültiger Wert für Konfiguration '%s'. Erwartet wird eine Liste von Bezeichnungen.".formatted(getKey()));
        }

        // Check if labels contains pipe character
        for (String label : labels) {
            if (label.contains("|")) {
                throw ResponseException.badRequest("Ungültiger Wert für Konfiguration '%s'. Bezeichnungen dürfen kein '|' enthalten.".formatted(getKey()));
            }
        }

        return String.join("|", labels);
    }
}
