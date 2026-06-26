package de.aivot.gover.backend.asset.configs;

import de.aivot.gover.backend.config.models.SystemConfigDefinition;
import de.aivot.gover.backend.elements.models.elements.BaseElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.gover.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.gover.backend.lib.exceptions.ResponseException;
import de.aivot.gover.backend.storage.enums.StorageProviderType;
import de.aivot.gover.backend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DefaultStorageAssetsSystemConfigDefinition implements SystemConfigDefinition<String> {
    public static final String DEFAULT_STORAGE_ASSETS_KEY = "storage.assets.default_storage_provider";

    private final StorageProviderRepository storageProviderRepository;

    public DefaultStorageAssetsSystemConfigDefinition(StorageProviderRepository storageProviderRepository) {
        this.storageProviderRepository = storageProviderRepository;
    }

    @Nonnull
    @Override
    public String getKey() {
        return DEFAULT_STORAGE_ASSETS_KEY;
    }

    @Nonnull
    @Override
    public BaseElement getConfigElement() {
        return new SelectInputElement()
                .setOptions(
                        storageProviderRepository
                                .findAllByType(StorageProviderType.Assets)
                                .stream()
                                .map((sp) -> SelectInputElementOption.of(sp.getId().toString(), sp.getName()))
                                .toList()
                )
                .setLabel(getLabel())
                .setHint(getDescription())
                .setId(getKey());
    }

    @Nonnull
    @Override
    public String getCategory() {
        return "Speicher";
    }

    @Nonnull
    @Override
    public String getLabel() {
        return "Standard Speicheranbieter für Assets";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Legt den Standard Speicheranbieter fest, der für das Speichern von Assets verwendet wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getKey() + ": " + value);
        }
        return value;
    }
}
