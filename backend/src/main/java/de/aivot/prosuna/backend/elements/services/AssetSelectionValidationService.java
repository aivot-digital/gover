package de.aivot.prosuna.backend.elements.services;

import de.aivot.prosuna.backend.asset.repositories.AssetRepository;
import de.aivot.prosuna.backend.elements.models.elements.form.input.AssetSelectInputElement;
import de.aivot.prosuna.backend.exceptions.ValidationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AssetSelectionValidationService {
    private final AssetRepository assetRepository;

    public AssetSelectionValidationService(@Nonnull AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /** Checks persisted membership after the element's intrinsic key validation has succeeded. */
    public void validate(@Nonnull AssetSelectInputElement element, @Nullable Object effectiveValue) throws ValidationException {
        var value = element.formatValue(effectiveValue);
        if (element.getStorageProviderId() == null || value == null) return;

        // Use persisted metadata rather than trusting the provider supplied by a picker or caller.
        var asset = assetRepository.findById(UUID.fromString(value))
                .orElseThrow(() -> new ValidationException(element, "Die ausgewählte Datei ist nicht verfügbar."));
        if (!element.getStorageProviderId().equals(asset.getStorageProviderId())) {
            throw new ValidationException(element, "Die ausgewählte Datei gehört nicht zum erlaubten Speicheranbieter.");
        }
    }
}
