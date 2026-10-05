package de.aivot.prosuna.backend.javascript.providers;

import de.aivot.prosuna.backend.asset.entities.AssetEntity;
import de.aivot.prosuna.backend.asset.services.AssetService;
import de.aivot.prosuna.backend.javascript.models.JavascriptCode;
import de.aivot.prosuna.backend.javascript.services.JavascriptEngine;
import de.aivot.prosuna.backend.plugins.core.v1.javascript.AssetJavascriptV1;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetJavascriptPluginTest {
    private static final UUID ASSET_KEY = UUID.fromString("3891538b-9058-4c3f-bb5b-0e318c77c70f");
    private static final byte[] ASSET_CONTENT = "Hello asset".getBytes(StandardCharsets.UTF_8);

    private AssetService assetService;
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        assetService = mock(AssetService.class);
        storageService = mock(StorageService.class);
    }

    @Test
    void getTextAndBase64() {
        try (var jsService = new JavascriptEngine(new AssetJavascriptV1(assetService, storageService))) {
            when(assetService.retrieve(ASSET_KEY))
                    .thenReturn(Optional.of(asset()));
            when(storageService.getDocumentContent(11, "assets/greeting.txt"))
                    .thenAnswer(invocation -> new ByteArrayInputStream(ASSET_CONTENT));

            var result = jsService.evaluateCode(new JavascriptCode().setCode("""
                    [
                        _assets_v1.getText('3891538b-9058-4c3f-bb5b-0e318c77c70f'),
                        _assets_v1.getBase64('3891538b-9058-4c3f-bb5b-0e318c77c70f')
                    ];
                    """));
            var values = assertInstanceOf(List.class, result.asObject());

            assertEquals("Hello asset", values.get(0));
            assertEquals(Base64.getEncoder().encodeToString(ASSET_CONTENT), values.get(1));
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void getDownloadUrl() {
        try (var jsService = new JavascriptEngine(new AssetJavascriptV1(assetService, storageService))) {
            var asset = asset();

            when(assetService.retrieve(ASSET_KEY))
                    .thenReturn(Optional.of(asset));
            when(assetService.createUrl(asset))
                    .thenReturn("https://example.org/api/public/assets/3891538b-9058-4c3f-bb5b-0e318c77c70f/");

            var result = jsService.evaluateCode(new JavascriptCode().setCode("_assets_v1.getDownloadUrl('3891538b-9058-4c3f-bb5b-0e318c77c70f');"));

            assertEquals("https://example.org/api/public/assets/3891538b-9058-4c3f-bb5b-0e318c77c70f/", result.asString());
        } catch (Exception e) {
            fail(e);
        }
    }

    private static AssetEntity asset() {
        return new AssetEntity()
                .setKey(ASSET_KEY)
                .setPrivate(false)
                .setStorageProviderId(11)
                .setStoragePathFromRoot("assets/greeting.txt");
    }

}
