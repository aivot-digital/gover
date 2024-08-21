package de.aivot.GoverBackend.services.storages;

import de.aivot.GoverBackend.models.entities.Asset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AssetStorageService {
    private final StorageService storageService;

    @Autowired
    public AssetStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    public String getAssetUrl(Asset asset) {
        return storageService.getRemoteFileUrl(getAssetPath(asset), asset.getFilename(), asset.getContentType());
    }

    public byte[] getAssetData(Asset asset) {
        return storageService.getFile(getAssetPath(asset));
    }

    public void saveAsset(Asset asset, byte[] data) {
        storageService.writeFile(getAssetPath(asset), data, asset.getContentType());
    }

    public void deleteAsset(Asset asset) {
        storageService.deleteFile(getAssetPath(asset));
    }

    private String getAssetPath(Asset asset) {
        String storagePrefix = "assets/";

        return storagePrefix + asset.getKey();
    }
}
