package de.aivot.prosuna.backend.system.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DashboardActivityEnabledSystemConfigDefinition implements SystemConfigDefinition<Boolean> {
    public static final String KEY = "dashboard.activity.enabled";

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public BaseElement getConfigElement() {
        return new CheckboxInputElement()
                .setVariant(CheckboxInputElement.VARIANT_SWITCH)
                .setLabel(getLabel())
                .setHint(getDescription())
                .setId(getKey());
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Übersicht";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Vorgangsaktivität anzeigen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Zeigt berechtigten Mitarbeiter:innen eine zusammengefasste Auswertung gestarteter und abgeschlossener Vorgänge auf der Übersicht.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        // The frontend needs this non-sensitive display preference before loading dashboard data to avoid layout shifts.
        return true;
    }

    @Nonnull
    @Override
    public Boolean getDefaultValue() {
        return true;
    }

    @Nonnull
    @Override
    public String serializeValueToDB(@Nullable Boolean value) {
        return Objects.requireNonNullElse(value, Boolean.TRUE).toString();
    }

    @Nonnull
    @Override
    public Boolean parseValueFromDB(@Nonnull String value) throws ResponseException {
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getLabel());
        }
        return Boolean.parseBoolean(value);
    }
}
