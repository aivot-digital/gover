package de.aivot.gover.backend.core.configs;

import de.aivot.gover.backend.config.models.SystemConfigDefinition;
import de.aivot.gover.backend.data.SystemConfigKey;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ProviderNameSystemConfigDefinition implements SystemConfigDefinition<String> {
    // TODO: Remove SystemConfigKey.PROVIDER__NAME and use the key directly
    public static final String KEY = SystemConfigKey.PROVIDER__NAME.getKey();

    @Nonnull
    @Override
    public String getKey() {
        return KEY;
    }

    @Nonnull
    @Override
    public BaseElement getConfigElement() {
        return new TextInputElement()
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
        return "Name des Anbieters";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Name des Anbieters, der in der Anwendung angezeigt wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        return value;
    }
}
