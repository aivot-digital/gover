package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PdfLogoServiceTest {
    @Test
    void resolveDataUrlEmbedsPublicImageFromStorage() throws Exception {
        var source = new byte[]{0, 1, 2, 3};
        var fixture = createFixture("image/png", source, false);

        var result = fixture.service().resolveDataUrl(fixture.assetKey());

        assertEquals(
                Optional.of("data:image/png;base64," + Base64.getEncoder().encodeToString(source)),
                result
        );
        verify(fixture.storageService()).getDocumentContent(1, "/appearance/logo");
    }

    @Test
    void resolveDataUrlOmitsPrivateAssetWithoutReadingIt() {
        var fixture = createFixture("image/png", new byte[]{0, 1, 2, 3}, true);

        var result = fixture.service().resolveDataUrl(fixture.assetKey());

        assertTrue(result.isEmpty());
        verifyNoInteractions(fixture.storageService());
    }

    @Test
    void resolveDataUrlOmitsNonImageAssetWithoutReadingIt() {
        var fixture = createFixture("text/plain", new byte[]{0, 1, 2, 3}, false);

        var result = fixture.service().resolveDataUrl(fixture.assetKey());

        assertTrue(result.isEmpty());
        verifyNoInteractions(fixture.storageService());
    }

    @Test
    void resolveDataUrlOmitsLogoWhenStorageReadFails() throws Exception {
        var fixture = createFixture("image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8), false);
        when(fixture.storageService().getDocumentContent(1, "/appearance/logo"))
                .thenThrow(new IllegalStateException("Storage unavailable"));

        var result = fixture.service().resolveDataUrl(fixture.assetKey());

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveDataUrlDoesNotAccessStorageWithoutAssetKey() {
        var repository = mock(VStorageIndexItemWithAssetRepository.class);
        var storageService = mock(StorageService.class);
        var service = new PdfLogoService(repository, storageService);

        assertTrue(service.resolveDataUrl(null).isEmpty());
        verifyNoInteractions(repository, storageService);
    }

    private TestFixture createFixture(String mimeType, byte[] source, boolean isPrivate) {
        var assetKey = UUID.randomUUID();
        var asset = new VStorageIndexItemWithAssetEntity(
                1,
                "/appearance/logo",
                false,
                "logo",
                mimeType,
                (long) source.length,
                false,
                StorageItemMetadata.empty(),
                Instant.parse("2026-09-14T10:00:00Z"),
                Instant.parse("2026-09-14T10:00:00Z"),
                assetKey,
                null,
                isPrivate
        );
        var repository = mock(VStorageIndexItemWithAssetRepository.class);
        var storageService = mock(StorageService.class);
        when(repository.findByAssetKey(assetKey)).thenReturn(Optional.of(asset));
        try {
            when(storageService.getDocumentContent(1, "/appearance/logo"))
                    .thenReturn(new ByteArrayInputStream(source));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return new TestFixture(new PdfLogoService(repository, storageService), assetKey, storageService);
    }

    private record TestFixture(PdfLogoService service, UUID assetKey, StorageService storageService) {
    }
}
