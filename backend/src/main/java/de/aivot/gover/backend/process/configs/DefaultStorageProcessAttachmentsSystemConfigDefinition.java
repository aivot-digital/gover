package de.aivot.gover.backend.process.configs;

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
public class DefaultStorageProcessAttachmentsSystemConfigDefinition implements SystemConfigDefinition<String> {
    // Needs to be changed also in the frontend, see storage-provider-details-page-index.tsx
    public static final String KEY = "storage.attachments.default_storage_provider";

    private final StorageProviderRepository storageProviderRepository;

    public DefaultStorageProcessAttachmentsSystemConfigDefinition(StorageProviderRepository storageProviderRepository) {
        this.storageProviderRepository = storageProviderRepository;
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
                        storageProviderRepository
                                .findAllByType(StorageProviderType.Attachments)
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
        return "Standard Speicheranbieter für Anhängen von Vorgängen";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Legt den Standard Speicheranbieter fest, der für das Speichern von Anhängen von Vorgängen verwendet wird.";
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
