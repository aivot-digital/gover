package de.aivot.prosuna.backend.asset.services;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.elements.enums.AssetVisibility;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetContentResolverServiceTest {
    private final AssetService assetService = mock(AssetService.class);
    private final StorageService storageService = mock(StorageService.class);
    private final AssetContentResolverService resolver = new AssetContentResolverService(assetService, storageService);

    @Test
    void readsContentFromTheAssetsCurrentStorageLocation() throws Exception {
        var assetKey = UUID.randomUUID();
        var content = "secret-content".getBytes(StandardCharsets.UTF_8);
        var asset = asset(assetKey, true, 42, "/credentials/client.pem");
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(42, "/credentials/client.pem"))
                .thenReturn(new ByteArrayInputStream(content));

        var result = resolver.resolveContent(assetKey.toString(), AssetVisibility.Private, "Das Zertifikat");

        assertArrayEquals(content, result);
        verify(storageService).getDocumentContent(42, "/credentials/client.pem");
    }

    @Test
    void rejectsPublicAssetWhenPrivateContentIsRequired() throws Exception {
        var assetKey = UUID.randomUUID();
        when(assetService.retrieve(assetKey)).thenReturn(Optional.of(asset(assetKey, false, 42, "/public.pem")));

        var exception = assertThrows(
                ResponseException.class,
                () -> resolver.resolveContent(assetKey.toString(), AssetVisibility.Private, "Der private Schlüssel")
        );

        assertEquals("Der private Schlüssel muss als private Datei hinterlegt sein.", exception.getMessage());
        verify(storageService, never()).getDocumentContent(42, "/public.pem");
    }

    @Test
    void rejectsMissingAndInvalidAssetReferencesBeforeReadingStorage() throws Exception {
        var invalidException = assertThrows(
                ResponseException.class,
                () -> resolver.resolveContent("not-an-asset-key", AssetVisibility.Private, "Das Zertifikat")
        );
        var missingKey = UUID.randomUUID();
        var missingException = assertThrows(
                ResponseException.class,
                () -> resolver.resolveContent(missingKey.toString(), AssetVisibility.Private, "Das Zertifikat")
        );

        assertEquals("Das Zertifikat ist ungültig konfiguriert.", invalidException.getMessage());
        assertEquals("Das Zertifikat wurde nicht gefunden.", missingException.getMessage());
        verify(storageService, never()).getDocumentContent(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    private static AssetEntity asset(UUID key, boolean isPrivate, int storageProviderId, String path) {
        return new AssetEntity()
                .setKey(key)
                .setPrivate(isPrivate)
                .setStorageProviderId(storageProviderId)
                .setStoragePathFromRoot(path);
    }
}
