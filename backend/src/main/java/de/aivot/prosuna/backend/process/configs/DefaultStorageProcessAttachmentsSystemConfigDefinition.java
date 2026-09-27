package de.aivot.prosuna.backend.process.configs;

import de.aivot.prosuna.backend.config.models.SystemConfigDefinition;
import de.aivot.prosuna.backend.elements.models.elements.BaseElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElement;
import de.aivot.prosuna.backend.elements.models.elements.form.input.SelectInputElementOption;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.repositories.ProcessInstanceRepository;
import de.aivot.prosuna.backend.storage.enums.StorageProviderType;
import de.aivot.prosuna.backend.storage.repositories.StorageProviderRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DefaultStorageProcessAttachmentsSystemConfigDefinition implements SystemConfigDefinition<String> {
    // Needs to be changed also in the frontend, see storage-provider-details-page-index.tsx
    public static final String KEY = "storage.attachments.default_storage_provider";

    private final StorageProviderRepository storageProviderRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    public DefaultStorageProcessAttachmentsSystemConfigDefinition(StorageProviderRepository storageProviderRepository,
                                                                  ProcessInstanceRepository processInstanceRepository) {
        this.storageProviderRepository = storageProviderRepository;
        this.processInstanceRepository = processInstanceRepository;
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

    @Override
    public void validateChange(@Nullable String oldValue,
                               @Nullable String newValue,
                               boolean changeConfirmed) throws ResponseException {
        if (Objects.equals(oldValue, newValue) || changeConfirmed) {
            return;
        }

        var runningProcesses = processInstanceRepository
                .countAllByStatusIs(ProcessInstanceStatus.Running);

        if (runningProcesses > 0) {
            throw ResponseException.conflict(
                    "Der zentrale Speicheranbieter für Vorgangsanlagen kann nicht ohne ausdrückliche Bestätigung geändert werden, weil aktuell %d Vorgänge laufen.",
                    runningProcesses
            );
        }
    }
}
