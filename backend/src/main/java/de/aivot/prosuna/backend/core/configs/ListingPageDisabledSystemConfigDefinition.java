package de.aivot.prosuna.backend.core.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.CheckboxInputElement;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ListingPageDisabledSystemConfigDefinition implements SystemConfigDefinition<Boolean> {
    public static final String KEY = "ProviderListingPageDisablePublicListingPage";

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
        return "Öffentliche Auflistung";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Öffentliche Auflistung deaktivieren";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Deaktiviert die öffentliche Auflistung der Formulare.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return true;
    }

    @Nonnull
    @Override
    public String serializeValueToDB(@Nullable Boolean value) throws ResponseException {
        return Objects
                .requireNonNullElse(value, Boolean.FALSE)
                .toString();
    }

    @Nullable
    @Override
    public Boolean parseValueFromDB(@Nonnull String value) throws ResponseException {
        return Boolean.parseBoolean(value);
    }
}
