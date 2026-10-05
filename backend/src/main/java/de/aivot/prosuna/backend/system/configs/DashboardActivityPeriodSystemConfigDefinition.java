package de.aivot.prosuna.backend.system.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.system.enums.DashboardActivityPeriod;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DashboardActivityPeriodSystemConfigDefinition implements SystemConfigDefinition<String> {
    public static final String KEY = "dashboard.activity.period";

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public BaseElement getConfigElement() {
        return new SelectInputElement()
                .setOptions(List.of(
                        SelectInputElementOption.of(DashboardActivityPeriod.ThirtyDays.getConfigValue(), "Letzte 30 Tage"),
                        SelectInputElementOption.of(DashboardActivityPeriod.ThreeMonths.getConfigValue(), "Letzte 3 Monate")
                ))
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
        return "Auswertungszeitraum";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Legt fest, für welchen Zeitraum die Vorgangsaktivität auf der Übersicht zusammengefasst wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        // The initial chart placeholder uses the configured period so its heading remains stable while data loads.
        return true;
    }

    @Nonnull
    @Override
    public String getDefaultValue() {
        return DashboardActivityPeriod.ThreeMonths.getConfigValue();
    }

    @Nonnull
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        return DashboardActivityPeriod.fromConfigValue(value).getConfigValue();
    }
}
