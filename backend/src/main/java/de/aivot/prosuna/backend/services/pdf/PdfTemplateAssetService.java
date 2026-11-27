package de.aivot.prosuna.backend.services.pdf;

import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.storage.services.StorageService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * Loads the content of the custom PDF template that is selected for a form.
 * <p>
 * The template is loaded only by the asset key stored in the form, never by a path or name. The content of that asset
 * can still be changed by everyone with the system permission {@code asset.update}, independent of the permission to
 * edit the form. This is an accepted trade-off that administrators must consider when granting {@code asset.update}.
 */
@Service
public class PdfTemplateAssetService {
    private static final Logger logger = LoggerFactory.getLogger(PdfTemplateAssetService.class);

    private final VStorageIndexItemWithAssetRepository assetRepository;
    private final StorageService storageService;

    public PdfTemplateAssetService(VStorageIndexItemWithAssetRepository assetRepository,
                                   StorageService storageService) {
        this.assetRepository = assetRepository;
        this.storageService = storageService;
    }

    /**
     * Returns the template content, or an empty result if the asset cannot be used, so the caller can fall back to the
     * bundled form template.
     */
    @Nonnull
    public Optional<String> loadTemplateContent(@Nullable UUID assetKey) {
        if (assetKey == null) {
            return Optional.empty();
        }

        try {
            var asset = assetRepository.findByAssetKey(assetKey).orElse(null);
            if (asset == null ||
                    Boolean.TRUE.equals(asset.getDirectory()) ||
                    Boolean.TRUE.equals(asset.getMissing())) {
                logger.warn("Could not load custom PDF template from unavailable asset {}", assetKey);
                return Optional.empty();
            }

            try (var inputStream = storageService.getDocumentContent(
                    asset.getStorageProviderId(),
                    asset.getPathFromRoot()
            )) {
                return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            logger.warn("Could not load custom PDF template from asset {}: {}", assetKey, exception.getMessage());
            logger.debug("Custom PDF template loading failure", exception);
            return Optional.empty();
        }
    }
}
