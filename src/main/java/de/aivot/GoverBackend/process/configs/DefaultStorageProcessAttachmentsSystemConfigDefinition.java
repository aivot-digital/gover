package de.aivot.GoverBackend.process.configs;

import de.aivot.GoverBackend.config.models.SystemConfigDefinition;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.GoverBackend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.enums.StorageProviderType;
import de.aivot.GoverBackend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

@Component
public class DefaultStorageProcessAttachmentsSystemConfigDefinition implements SystemConfigDefinition<String> {
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
