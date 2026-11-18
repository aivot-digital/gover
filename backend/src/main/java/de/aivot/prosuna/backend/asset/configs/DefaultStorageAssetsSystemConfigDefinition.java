package de.aivot.prosuna.backend.asset.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DefaultStorageAssetsSystemConfigDefinition implements SystemConfigDefinition<String> {
    // Needs to be changed also in the frontend, see storage-provider-details-page-index.tsx
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
        return "Standard-Speicheranbieter für Dateien und Medien";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Legt den Standard-Speicheranbieter fest, der für das Speichern von Dateien und Medien verwendet wird.";
    }

    @Nonnull
    @Override
    public Boolean isPublicConfig() {
        return false;
    }

    @Nullable
    @Override
    public String parseValueFromDB(@Nonnull String value) throws ResponseException {
        // Without a stored value, the config falls back to an empty value. This is the case if the local storage
        // providers were not seeded and no administrator has selected a default provider yet.
        if (value.isBlank()) {
            return null;
        }

        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ResponseException.internalServerError("Ungültiger Wert für " + getKey() + ": " + value);
        }
        return value;
    }
}
