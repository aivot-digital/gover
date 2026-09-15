package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.storage.services.StorageService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves theme logos as inline image sources for PDF rendering.
 */
@Service
public class PdfLogoService {
    private static final Logger logger = LoggerFactory.getLogger(PdfLogoService.class);

    private final VStorageIndexItemWithAssetRepository assetRepository;
    private final StorageService storageService;

    public PdfLogoService(VStorageIndexItemWithAssetRepository assetRepository,
                          StorageService storageService) {
        this.assetRepository = assetRepository;
        this.storageService = storageService;
    }

    /**
     * Loads a public image asset and returns a data URL that Gotenberg can render without an external request.
     */
    @Nonnull
    public Optional<String> resolveDataUrl(@Nullable UUID assetKey) {
        if (assetKey == null) {
            return Optional.empty();
        }

        try {
            var asset = assetRepository.findByAssetKey(assetKey).orElse(null);
            if (asset == null ||
                    Boolean.TRUE.equals(asset.getDirectory()) ||
                    Boolean.TRUE.equals(asset.getMissing()) ||
                    !Boolean.FALSE.equals(asset.getAssetIsPrivate())) {
                logger.warn("Could not prepare PDF logo for unavailable public asset {}", assetKey);
                return Optional.empty();
            }

            var mediaType = MediaType.parseMediaType(asset.getMimeType());
            if (!"image".equalsIgnoreCase(mediaType.getType())) {
                logger.warn("Could not prepare PDF logo for non-image asset {}", assetKey);
                return Optional.empty();
            }

            try (var inputStream = storageService.getDocumentContent(
                    asset.getStorageProviderId(),
                    asset.getPathFromRoot()
            )) {
                var encodedContent = Base64.getEncoder().encodeToString(inputStream.readAllBytes());
                return Optional.of("data:" + mediaType + ";base64," + encodedContent);
            }
        } catch (Exception exception) {
            logger.warn("Could not prepare PDF logo for asset {}: {}", assetKey, exception.getMessage());
            logger.debug("PDF logo preparation failure", exception);
            return Optional.empty();
        }
    }
}
