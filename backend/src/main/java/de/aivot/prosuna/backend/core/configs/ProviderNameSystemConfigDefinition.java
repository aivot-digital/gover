package de.aivot.prosuna.backend.core.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.data.SystemConfigKey;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.TextInputElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
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
        return "Name der Organisation";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Name der Organisation, die diese Prosuna-Instanz einsetzt. " +
                "Dieser Name wird in der Anwendung angezeigt und ist auch für externe Beteiligte sichtbar, " +
                "beispielsweise im Formularverzeichnis und in Formularen. " +
                "Änderungen am Namen werden erst nach dem erneuten Laden der Anwendung in allen Bereichen sichtbar.";
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
