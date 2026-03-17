package de.aivot.GoverBackend.plugins.core.v1.javascript;

import de.aivot.GoverBackend.asset.entities.AssetEntity;
import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugins.core.Core;
import de.aivot.GoverBackend.storage.services.StorageService;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.HostAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.UUID;

@Component
public class AssetJavascriptV1 implements JavascriptFunctionProvider {
    private final AssetService assetService;
    private final StorageService storageService;

    @Autowired
    public AssetJavascriptV1(
            AssetService assetService,
            @Lazy StorageService storageService) {
        this.assetService = assetService;
        this.storageService = storageService;
    }

    @Nonnull
    @Override
    public String getComponentKey() {
        return "assets";
    }

    @Nonnull
    @Override
    public String getComponentVersion() {
        return "1.0.0";
    }

    @Nonnull
    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Nonnull
    @Override
    public String getName() {
        return "Dateien & Medien";
    }

    @Nonnull
    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Dateien & Medien.";
    }

    @Override
    public String[] getMethodTypeDefinitions() {
        return new String[]{
                "getBytes(assetKey: string): ArrayBuffer;",
                "getText(assetKey: string): string;",
                "getBase64(assetKey: string): string;",
                "getDownloadUrl(assetKey: string): string;"
        };
    }

    @HostAccess.Export
    public byte[] getBytes(String assetKey) throws IOException {
        AssetEntity assetObj;
        try {
            assetObj = assetService
                    .retrieve(parseUUID(assetKey))
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetKey));
        } catch (ResponseException e) {
            throw new IOException(e);
        }

        byte[] assetBytes;
        try {
            var is = storageService
                    .getDocumentContent(assetObj.getStorageProviderId(), assetObj.getStoragePathFromRoot());
            assetBytes = is.readAllBytes();
        } catch (Exception e) {
            throw new IOException(e);
        }

        return assetBytes;
    }

    @HostAccess.Export
    public String getText(String assetKey) throws IOException {
        var bytes = getBytes(assetKey);
        return new String(bytes, Charset.defaultCharset());
    }

    @HostAccess.Export
    public String getBase64(String assetKey) throws IOException {
        var bytes = getBytes(assetKey);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @HostAccess.Export
    public String getDownloadUrl(String assetKey) throws IOException {
        AssetEntity assetObj;
        try {
            assetObj = assetService
                    .retrieve(parseUUID(assetKey))
                    .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetKey));
        } catch (ResponseException e) {
            throw new IOException(e);
        }

        return assetService.createUrl(assetObj);
    }

    private UUID parseUUID(String assetKey) {
        try {
            return UUID.fromString(assetKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid asset key: " + assetKey);
        }
    }
}
