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
public class MarketplaceKeySystemConfigDefinition implements SystemConfigDefinition<String> {
    // TODO: Remove SystemConfigKey.PROSUNA_MARKETPLACE_KEY and use the key directly
    public static final String KEY = SystemConfigKey.PROSUNA_MARKETPLACE_KEY.getKey();

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
        return "Prosuna Marktplatz";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Marktplatz-Schlüssel";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Schlüssel für den Zugriff auf den Prosuna Marktplatz.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        return value;
    }
}
