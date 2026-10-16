package de.aivot.prosuna.backend.asset.controllers;

import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntityId;
import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.lib.exceptions.ResponseException;
import de.aivot.prosuna.backend.storage.services.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerAssetControllerTest {
    private static final String SANDBOX_POLICY = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    @Mock
    private StorageService storageService;

    @Mock
    private VStorageIndexItemWithAssetRepository vStorageIndexItemWithAssetRepository;

    @Mock
    private HttpServletRequest request;

    private CustomerAssetController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        controller = new CustomerAssetController(
                storageService,
                vStorageIndexItemWithAssetRepository
        );
    }

    @Test
    void retrievePublicByPathPreservesPlusInFilePath() {
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/public/assets/42/files/folder/a+b.txt"));
        when(vStorageIndexItemWithAssetRepository.findById(VStorageIndexItemWithAssetEntityId.of(42, "/folder/a+b.txt")))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseException.class,
                () -> controller.retrievePublicByPath(42, false, request)
        );

        verify(vStorageIndexItemWithAssetRepository)
                .findById(VStorageIndexItemWithAssetEntityId.of(42, "/folder/a+b.txt"));
    }

    @ParameterizedTest
    @CsvSource({
            "page.html, text/html",
            "logo.svg, image/svg+xml",
            "data.xml, application/xml",
            "page.xhtml, application/xhtml+xml",
            "unknown.bin, not a media type",
    })
    void retrievePublicByIdServesActiveContentAsSandboxedAttachment(String filename, String mimeType) throws Exception {
        var assetKey = mockPublicAsset(filename, mimeType);

        var response = controller.retrievePublicById(assetKey.toString(), false);

        var headers = response.getHeaders();
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        assertEquals(SANDBOX_POLICY, headers.getFirst("Content-Security-Policy"));
    }

    @Test
    void retrievePublicByIdKeepsTheMediaTypeOfActiveContentForEmbeddedImages() throws Exception {
        var assetKey = mockPublicAsset("logo.svg", "image/svg+xml");

        var response = controller.retrievePublicById(assetKey.toString(), false);

        assertEquals("image/svg+xml", response.getHeaders().getContentType().toString());
    }

    @ParameterizedTest
    @CsvSource({
            "logo.png, image/png",
            "photo.jpg, image/jpeg",
            "animation.gif, image/gif",
            "photo.webp, image/webp",
            "document.pdf, application/pdf",
            "notes.txt, text/plain;charset=UTF-8",
    })
    void retrievePublicByIdDisplaysPassiveContentInline(String filename, String mimeType) throws Exception {
        var assetKey = mockPublicAsset(filename, mimeType);

        var response = controller.retrievePublicById(assetKey.toString(), false);

        var headers = response.getHeaders();
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline;"));
        assertFalse(headers.containsHeader("Content-Security-Policy"));
    }

    @Test
    void retrievePublicByIdDownloadsPassiveContentOnRequest() throws Exception {
        var assetKey = mockPublicAsset("logo.png", "image/png");

        var response = controller.retrievePublicById(assetKey.toString(), true);

        var headers = response.getHeaders();
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        assertFalse(headers.containsHeader("Content-Security-Policy"));
    }

    @Test
    void retrievePublicByPathServesActiveContentAsSandboxedAttachment() throws Exception {
        when(request.getRequestURL())
                .thenReturn(new StringBuffer("http://localhost/api/public/assets/42/files/folder/page.html"));
        when(vStorageIndexItemWithAssetRepository.findById(VStorageIndexItemWithAssetEntityId.of(42, "/folder/page.html")))
                .thenReturn(Optional.of(createPublicAsset("page.html", "text/html")));
        when(storageService.getDocumentContent(42, "/folder/page.html"))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        var response = controller.retrievePublicByPath(42, false, request);

        var headers = response.getHeaders();
        assertTrue(headers.getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment;"));
        assertEquals(SANDBOX_POLICY, headers.getFirst("Content-Security-Policy"));
    }

    private UUID mockPublicAsset(String filename, String mimeType) throws ResponseException {
        var assetKey = UUID.randomUUID();
        var asset = createPublicAsset(filename, mimeType);

        when(vStorageIndexItemWithAssetRepository.findByAssetKey(assetKey))
                .thenReturn(Optional.of(asset));
        when(storageService.getDocumentContent(42, asset.getPathFromRoot()))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        return assetKey;
    }

    private static VStorageIndexItemWithAssetEntity createPublicAsset(String filename, String mimeType) {
        return new VStorageIndexItemWithAssetEntity()
                .setStorageProviderId(42)
                .setPathFromRoot("/folder/" + filename)
                .setFilename(filename)
                .setMimeType(mimeType)
                .setDirectory(false)
                .setMissing(false)
                .setAssetIsPrivate(false);
    }
}
