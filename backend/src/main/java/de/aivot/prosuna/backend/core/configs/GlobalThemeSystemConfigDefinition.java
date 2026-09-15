package de.aivot.prosuna.backend.core.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.theme.repositories.ThemeRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class GlobalThemeSystemConfigDefinition implements SystemConfigDefinition<String> {
    public static final String KEY = "SystemTheme";

    private final ThemeRepository themeRepository;

    public GlobalThemeSystemConfigDefinition(ThemeRepository themeRepository) {
        this.themeRepository = themeRepository;
    }

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public BaseElement getConfigElement() {
        return new SelectInputElement()
                .setOptions(
                        themeRepository
                                .findAll()
                                .stream()
                                .map(theme -> SelectInputElementOption.of(theme.getId().toString(), theme.getName()))
                                .toList()
                )
                .setLabel(getLabel())
                .setHint(getDescription())
                .setId(getKey());
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Oberfläche";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Standard-Erscheinungsbild";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Das Standard-Erscheinungsbild der Prosuna-Instanz. Es wird überall dort genutzt, wo kein spezifischeres Erscheinungsbild einer Organisationseinheit greift.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        // Optional references use an empty string to represent "not configured" in system_configs.
        if (value.isEmpty()) {
            return value;
        }

        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getLabel());
        }
        return value;
    }

    @Override
    public void validate(@Nullable String value) throws ResponseException {
        if (value == null || value.isEmpty()) {
            return;
        }

        if (!themeRepository.existsById(Integer.parseInt(value))) {
            throw ResponseException.badRequest("Das ausgewählte Erscheinungsbild existiert nicht.");
        }
    }
}
