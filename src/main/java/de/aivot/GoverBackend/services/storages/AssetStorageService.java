package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetStorageService {
    private final StorageService storageService;

    @Autowired
    public AssetStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public String getAssetUrl(AssetEntity asset) throws ResponseException {
        return storageService.getRemoteFileUrl(getAssetPath(asset), asset.getFilename(), asset.getContentType());
    }

    public String getAssetDownloadUrl(AssetEntity asset) throws ResponseException {
        return storageService.getRemoteFileDownloadUrl(getAssetPath(asset), asset.getFilename(), asset.getContentType());
    }

    public byte[] getAssetData(AssetEntity asset) throws ResponseException {
        return storageService.getFile(getAssetPath(asset));
    }

    public void saveAsset(AssetEntity asset, byte[] data) throws ResponseException {
        storageService.writeFile(getAssetPath(asset), data, asset.getContentType());
    }

    public void deleteAsset(AssetEntity asset) throws ResponseException {
        storageService.deleteFile(getAssetPath(asset));
    }

    private String getAssetPath(AssetEntity asset) {
        String storagePrefix = "assets/";

        return storagePrefix + asset.getKey();
    }
}
