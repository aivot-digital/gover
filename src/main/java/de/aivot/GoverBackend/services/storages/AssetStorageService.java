package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.storage.models.StorageItemMetadata;
import de.aivot.GoverBackend.storage.services.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AssetStorageService {
    private final LegacyStorageService legacyStorageService;
    private final StorageService storageService;

    @Autowired
    public AssetStorageService(LegacyStorageService legacyStorageService,
                               StorageService storageService) {
        this.legacyStorageService = legacyStorageService;
        this.storageService = storageService;
    }

    public String getAssetUrl(AssetEntity asset) throws ResponseException {
        return legacyStorageService.getRemoteFileUrl(getLegacyAssetPath(asset), asset.getFilename(), asset.getContentType());
    }

    public String getAssetDownloadUrl(AssetEntity asset) throws ResponseException {
        return legacyStorageService.getRemoteFileDownloadUrl(getLegacyAssetPath(asset), asset.getFilename(), asset.getContentType());
    }

    public byte[] getAssetData(AssetEntity asset) throws ResponseException {
        if (hasStorageReference(asset)) {
            try (var inputStream = storageService.getDocumentContent(asset.getStorageProviderId(), asset.getStoragePathFromRoot())) {
                return inputStream.readAllBytes();
            } catch (IOException e) {
                throw ResponseException.internalServerError("Die Asset-Datei konnte nicht gelesen werden.", e);
            }
        }

        return legacyStorageService.getFile(getLegacyAssetPath(asset));
    }

    public void saveAsset(AssetEntity asset, byte[] data) throws ResponseException {
        if (hasStorageReference(asset)) {
            storageService.storeDocument(
                    asset.getStorageProviderId(),
                    asset.getStoragePathFromRoot(),
                    data,
                    StorageItemMetadata.empty()
            );
            return;
        }

        legacyStorageService.writeFile(getLegacyAssetPath(asset), data, asset.getContentType());
    }

    public void deleteAsset(AssetEntity asset) throws ResponseException {
        if (hasStorageReference(asset)) {
            storageService.deleteDocument(asset.getStorageProviderId(), asset.getStoragePathFromRoot());
            return;
        }

        legacyStorageService.deleteFile(getLegacyAssetPath(asset));
    }

    private static String getLegacyAssetPath(AssetEntity asset) {
        String storagePrefix = "assets/";

        return storagePrefix + asset.getKey();
    }

    private static boolean hasStorageReference(AssetEntity asset) {
        return asset.getStorageProviderId() != null &&
                asset.getStoragePathFromRoot() != null &&
                !asset.getStoragePathFromRoot().isBlank();
    }
}
