package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.asset.entities.VStorageIndexItemWithAssetEntity;
import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.storage.models.StorageItemMetadata;
import de.aivot.prosuna.backend.storage.services.StorageService;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailLogoServiceTest {
    @Test
    void createSenderLogoRasterizesSvgToBoundedPng() throws Exception {
        var source = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 100">
                    <rect width="200" height="100"
                          style="fill:#FF613A;fill:color(display-p3 1.0000 0.3804 0.2275);fill-opacity:1;"/>
                </svg>
                """.getBytes(StandardCharsets.UTF_8);
        var fixture = createFixture("image/svg+xml", source);

        var result = fixture.service().createSenderLogo(fixture.assetKey()).orElseThrow();
        var image = ImageIO.read(new ByteArrayInputStream(result.bytes()));

        assertEquals("image/png", result.contentType());
        assertEquals("sender-logo.png", result.filename());
        assertNotNull(image);
        assertEquals(400, image.getWidth());
        assertEquals(200, image.getHeight());
        assertEquals(2d, (double) image.getWidth() / image.getHeight(), 0.02d);
        assertEquals(0, new Color(image.getRGB(0, 0), true).getAlpha());
        assertEquals(Color.WHITE, new Color(image.getRGB(8, 100), true));
        assertEquals(new Color(255, 97, 58), new Color(image.getRGB(200, 100), true));
    }

    @Test
    void createSenderLogoScalesRasterImagesWithoutChangingTheirAspectRatio() throws Exception {
        var sourceImage = new BufferedImage(1000, 100, BufferedImage.TYPE_INT_RGB);
        var graphics = sourceImage.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, sourceImage.getWidth(), sourceImage.getHeight());
        graphics.dispose();
        var source = new ByteArrayOutputStream();
        ImageIO.write(sourceImage, "jpg", source);
        var fixture = createFixture("image/jpeg", source.toByteArray());

        var result = fixture.service().createSenderLogo(fixture.assetKey()).orElseThrow();
        var image = ImageIO.read(new ByteArrayInputStream(result.bytes()));

        assertEquals(400, image.getWidth());
        assertEquals(200, image.getHeight());
        var centerColor = new Color(image.getRGB(200, 100), true);
        assertTrue(centerColor.getRed() >= 250);
        assertTrue(centerColor.getGreen() >= 195 && centerColor.getGreen() <= 205);
        assertTrue(centerColor.getBlue() <= 5);
    }

    @Test
    void createSenderLogoRejectsSvgDocumentTypesAndCachesTheFailure() throws Exception {
        var source = """
                <!DOCTYPE svg [<!ENTITY external SYSTEM "file:///etc/passwd">]>
                <svg xmlns="http://www.w3.org/2000/svg"><text>&external;</text></svg>
                """.getBytes(StandardCharsets.UTF_8);
        var fixture = createFixture("image/svg+xml", source);

        assertTrue(fixture.service().createSenderLogo(fixture.assetKey()).isEmpty());
        assertTrue(fixture.service().createSenderLogo(fixture.assetKey()).isEmpty());
        verify(fixture.storageService(), times(1)).getDocumentContent(1, "/appearance/logo");
    }

    private TestFixture createFixture(String mimeType, byte[] source) {
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
                Instant.parse("2026-08-12T10:00:00Z"),
                Instant.parse("2026-08-12T10:00:00Z"),
                assetKey,
                null,
                false
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

        return new TestFixture(new MailLogoService(repository, storageService), assetKey, storageService);
    }

    private record TestFixture(MailLogoService service, UUID assetKey, StorageService storageService) {
    }
}
