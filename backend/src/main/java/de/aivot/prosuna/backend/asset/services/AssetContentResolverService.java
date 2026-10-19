package de.aivot.prosuna.backend.asset.services;

import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.services.StorageService;
import de.aivot.prosuna.backend.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

/** Resolves stable asset keys to the current storage location and reads their content. */
@Service
public class AssetContentResolverService {
    private final AssetService assetService;
    private final StorageService storageService;

    public AssetContentResolverService(AssetService assetService, StorageService storageService) {
        this.assetService = assetService;
        this.storageService = storageService;
    }

    @Nonnull
    public byte[] resolveContent(@Nullable String rawAssetKey,
                                 @Nonnull AssetVisibility requiredVisibility,
                                 @Nonnull String description) throws ResponseException {
        var normalizedAssetKey = StringUtils.toNullableTrimmedString(rawAssetKey);
        if (normalizedAssetKey == null) {
            throw ResponseException.internalServerError(description + " ist nicht konfiguriert.");
        }

        final UUID assetKey;
        try {
            assetKey = UUID.fromString(normalizedAssetKey);
            if (!assetKey.toString().equalsIgnoreCase(normalizedAssetKey)) {
                throw new IllegalArgumentException("Non-canonical UUID");
            }
        } catch (IllegalArgumentException e) {
            throw ResponseException.internalServerError(description + " ist ungültig konfiguriert.", e);
        }

        var asset = assetService
                .retrieve(assetKey)
                .orElseThrow(() -> ResponseException.internalServerError(description + " wurde nicht gefunden."));

        if (requiredVisibility == AssetVisibility.Private && !asset.getPrivate()) {
            throw ResponseException.internalServerError(description + " muss als private Datei hinterlegt sein.");
        }
        if (requiredVisibility == AssetVisibility.Public && asset.getPrivate()) {
            throw ResponseException.internalServerError(description + " muss öffentlich erreichbar sein.");
        }

        try (var content = storageService.getDocumentContent(
                asset.getStorageProviderId(),
                asset.getStoragePathFromRoot()
        )) {
            return content.readAllBytes();
        } catch (IOException e) {
            throw ResponseException.internalServerError(description + " konnte nicht gelesen werden.", e);
        }
    }
}
