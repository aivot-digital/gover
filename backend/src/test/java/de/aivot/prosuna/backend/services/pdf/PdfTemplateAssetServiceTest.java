package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PdfTemplateAssetServiceTest {
    private static final String TEMPLATE_PATH = "/Vorlagen/Antrag.html";

    @Test
    void loadTemplateContentReadsSelectedAssetFromStorage() {
        var template = "<p th:text=\"${form.publicTitle}\">Prüfung</p>";
        var fixture = createFixture(false, false, template);

        var result = fixture.service().loadTemplateContent(fixture.assetKey());

        assertEquals(Optional.of(template), result);
    }

    @Test
    void loadTemplateContentOmitsMissingAssetWithoutReadingIt() {
        var fixture = createFixture(false, true, "<p>Vorlage</p>");

        var result = fixture.service().loadTemplateContent(fixture.assetKey());

        assertTrue(result.isEmpty());
        verifyNoInteractions(fixture.storageService());
    }

    @Test
    void loadTemplateContentOmitsDirectoryWithoutReadingIt() {
        var fixture = createFixture(true, false, "<p>Vorlage</p>");

        var result = fixture.service().loadTemplateContent(fixture.assetKey());

        assertTrue(result.isEmpty());
        verifyNoInteractions(fixture.storageService());
    }

    @Test
    void loadTemplateContentOmitsTemplateWhenStorageReadFails() throws Exception {
        var fixture = createFixture(false, false, "<p>Vorlage</p>");
        when(fixture.storageService().getDocumentContent(1, TEMPLATE_PATH))
                .thenThrow(new IllegalStateException("Storage unavailable"));

        var result = fixture.service().loadTemplateContent(fixture.assetKey());

        assertTrue(result.isEmpty());
    }

    @Test
    void loadTemplateContentOmitsUnknownAsset() {
        var repository = mock(VStorageIndexItemWithAssetRepository.class);
        var storageService = mock(StorageService.class);
        var service = new PdfTemplateAssetService(repository, storageService);

        assertTrue(service.loadTemplateContent(UUID.randomUUID()).isEmpty());
        verifyNoInteractions(storageService);
    }

    @Test
    void loadTemplateContentDoesNotAccessStorageWithoutAssetKey() {
        var repository = mock(VStorageIndexItemWithAssetRepository.class);
        var storageService = mock(StorageService.class);
        var service = new PdfTemplateAssetService(repository, storageService);

        assertTrue(service.loadTemplateContent(null).isEmpty());
        verifyNoInteractions(repository, storageService);
    }

    private TestFixture createFixture(boolean directory, boolean missing, String content) {
        var assetKey = UUID.randomUUID();
        var source = content.getBytes(StandardCharsets.UTF_8);
        var asset = new VStorageIndexItemWithAssetEntity(
                1,
                TEMPLATE_PATH,
                directory,
                "Antrag.html",
                "text/html",
                (long) source.length,
                missing,
                StorageItemMetadata.empty(),
                Instant.parse("2026-10-13T10:00:00Z"),
                Instant.parse("2026-10-13T10:00:00Z"),
                assetKey,
                null,
                true
        );
        var repository = mock(VStorageIndexItemWithAssetRepository.class);
        var storageService = mock(StorageService.class);
        when(repository.findByAssetKey(assetKey)).thenReturn(Optional.of(asset));
        try {
            when(storageService.getDocumentContent(1, TEMPLATE_PATH))
                    .thenReturn(new ByteArrayInputStream(source));
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }

        return new TestFixture(new PdfTemplateAssetService(repository, storageService), assetKey, storageService);
    }

    private record TestFixture(PdfTemplateAssetService service, UUID assetKey, StorageService storageService) {
    }
}
