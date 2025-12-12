package de.aivot.GoverBackend.plugins.corePlugin.components.javascript;

import de.aivot.GoverBackend.asset.services.AssetService;
import de.aivot.GoverBackend.javascript.providers.JavascriptFunctionProvider;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.plugin.models.PluginComponent;
import de.aivot.GoverBackend.plugins.corePlugin.Core;
import de.aivot.GoverBackend.services.storages.AssetStorageService;
import org.graalvm.polyglot.HostAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.UUID;

@Component
public class AssetJavascript implements JavascriptFunctionProvider, PluginComponent {
    private final AssetService assetService;
    private final AssetStorageService assetStorageService;

    @Autowired
    public AssetJavascript(
            AssetService assetService,
            AssetStorageService assetStorageService
    ) {
        this.assetService = assetService;
        this.assetStorageService = assetStorageService;
    }

    @Override
    public String getKey() {
        return "assets";
    }

    @Override
    public String getParentPluginKey() {
        return Core.PLUGIN_KEY;
    }

    @Override
    public String getName() {
        return "Dateien & Medien";
    }

    @Override
    public String getDescription() {
        return "Dieses Paket enthält Funktionen für Dateien & Medien.";
    }

    @Override
    public String getObjectName() {
        return "";
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
        var assetObj = assetService
                .retrieve(parseUUID(assetKey))
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetKey));

        byte[] assetBytes;
        try {
            assetBytes = assetStorageService
                    .getAssetData(assetObj);
        } catch (ResponseException e) {
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
        var assetObj = assetService
                .retrieve(parseUUID(assetKey))
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetKey));

        try {
            return assetStorageService.getAssetDownloadUrl(assetObj);
        } catch (ResponseException e) {
            throw new IOException(e);
        }
    }

    private UUID parseUUID(String assetKey) {
        try {
            return UUID.fromString(assetKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid asset key: " + assetKey);
        }
    }
}
