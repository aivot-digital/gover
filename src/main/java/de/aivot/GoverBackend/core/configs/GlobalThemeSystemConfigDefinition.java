package de.aivot.GoverBackend.core.configs;

import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.theme.repositories.ThemeRepository;
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

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getLabel());
        }
        return value;
    }
}
