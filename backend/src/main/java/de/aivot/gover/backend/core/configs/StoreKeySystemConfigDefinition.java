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
public class StoreKeySystemConfigDefinition implements SystemConfigDefinition<String> {
    // TODO: Remove SystemConfigKey.GOVER__STORE_KEY and use the key directly
    public static final String KEY = SystemConfigKey.GOVER__STORE_KEY.getKey();

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
        return "Gover Store";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Store-Schlüssel";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Der Schlüssel für den Zugriff auf den Gover Store.";
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
