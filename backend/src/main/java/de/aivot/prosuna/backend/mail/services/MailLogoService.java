package de.aivot.prosuna.backend.mail.services;

import de.aivot.prosuna.backend.asset.repositories.VStorageIndexItemWithAssetRepository;
import de.aivot.prosuna.backend.storage.services.StorageService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.helpers.DefaultHandler;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Prepares the configured sender logo for reliable inline rendering across mail clients.
 */
@Service
public class MailLogoService {
    private static final Logger logger = LoggerFactory.getLogger(MailLogoService.class);
    private static final int EMAIL_LOGO_MAX_WIDTH = 400;
    private static final int EMAIL_LOGO_MAX_HEIGHT = 200;
    private static final int EMAIL_LOGO_PADDING = 16;
    private static final int EMAIL_LOGO_CORNER_DIAMETER = 32;
    private static final Color EMAIL_LOGO_BACKGROUND = Color.WHITE;
    private static final int MAX_SOURCE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_SOURCE_PIXELS = 40_000_000L;
    private static final int MAX_CACHE_ENTRIES = 64;
    private static final Pattern UNSUPPORTED_DISPLAY_P3_DECLARATION = Pattern.compile(
            "(?i)(^|[;{])\\s*[a-z-]+\\s*:\\s*color\\(\\s*display-p3\\b[^)]*\\)\\s*(?:!important\\s*)?;?"
    );

    private final VStorageIndexItemWithAssetRepository assetRepository;
    private final StorageService storageService;
    private final Map<CacheKey, Optional<SenderLogo>> senderLogoCache = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_CACHE_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<CacheKey, Optional<SenderLogo>> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            }
    );

    public MailLogoService(VStorageIndexItemWithAssetRepository assetRepository,
                           StorageService storageService) {
        this.assetRepository = assetRepository;
        this.storageService = storageService;
    }

    /**
     * Creates a bounded PNG suitable for inline mail rendering. The neutral surface is part of the bitmap so that
     * clients cannot independently invert it, while transparent rounded corners avoid a hard rectangular container.
     */
    @Nonnull
    public Optional<SenderLogo> createSenderLogo(@Nullable UUID assetKey) {
        if (assetKey == null) {
            return Optional.empty();
        }

        var asset = assetRepository.findByAssetKey(assetKey).orElse(null);
        if (asset == null || asset.getDirectory() || asset.getMissing()) {
            return Optional.empty();
        }

        // Replacing an asset changes its timestamp and therefore creates a fresh entry. The bounded LRU map retires
        // superseded sender logos without requiring cache invalidation hooks in every storage implementation.
        var cacheKey = new CacheKey(assetKey, asset.getUpdated());
        var cached = senderLogoCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try (var inputStream = storageService.getDocumentContent(
                asset.getStorageProviderId(),
                asset.getPathFromRoot()
        )) {
            var source = inputStream.readNBytes(MAX_SOURCE_BYTES + 1);
            if (source.length > MAX_SOURCE_BYTES) {
                throw new IOException("Image exceeds the maximum source size");
            }

            byte[] normalizedLogo = MediaType.valueOf(asset.getMimeType()).isCompatibleWith(MediaType.valueOf("image/svg+xml"))
                    ? renderSvg(source)
                    : renderRaster(source);
            byte[] png = addEmailLogoSurface(normalizedLogo);
            var senderLogo = new SenderLogo(png, MediaType.IMAGE_PNG_VALUE, "sender-logo.png");
            senderLogoCache.put(cacheKey, Optional.of(senderLogo));
            return Optional.of(senderLogo);
        } catch (Exception exception) {
            // Cache failures for this exact asset version to avoid repeated parsing attempts and log noise per email.
            senderLogoCache.put(cacheKey, Optional.empty());
            logger.warn(
                    "Could not prepare sender logo for asset {}: {}",
                    assetKey,
                    exception.getMessage()
            );
            logger.debug("Sender logo preparation failure", exception);
            return Optional.empty();
        }
    }

    @Nonnull
    private byte[] renderSvg(@Nonnull byte[] source) throws Exception {
        var document = parseAndNormalizeSvg(source);

        var transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) EMAIL_LOGO_MAX_WIDTH);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) EMAIL_LOGO_MAX_HEIGHT);
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_EXECUTE_ONLOAD, Boolean.FALSE);
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_CONSTRAIN_SCRIPT_ORIGIN, Boolean.TRUE);
        transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_ALLOW_EXTERNAL_RESOURCES, Boolean.FALSE);

        try (var output = new ByteArrayOutputStream()) {
            transcoder.transcode(
                    new TranscoderInput(document),
                    new TranscoderOutput(output)
            );
            return output.toByteArray();
        }
    }

    @Nonnull
    private Document parseAndNormalizeSvg(@Nonnull byte[] source) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler());
        var document = builder.parse(new ByteArrayInputStream(source));

        // Some design tools add an sRGB declaration followed by a CSS Color 4 display-p3 override. Batik does not
        // support the latter, so remove only that override and retain the preceding compatible fallback.
        var elements = document.getElementsByTagName("*");
        for (var index = 0; index < elements.getLength(); index++) {
            if (!(elements.item(index) instanceof Element element)) {
                continue;
            }

            if (element.hasAttribute("style")) {
                element.setAttribute("style", removeUnsupportedDisplayP3Declarations(element.getAttribute("style")));
            }
            if ("style".equals(element.getLocalName()) || "style".equals(element.getTagName())) {
                element.setTextContent(removeUnsupportedDisplayP3Declarations(element.getTextContent()));
            }
        }

        return document;
    }

    @Nonnull
    private String removeUnsupportedDisplayP3Declarations(@Nonnull String css) {
        return UNSUPPORTED_DISPLAY_P3_DECLARATION.matcher(css).replaceAll("$1");
    }

    @Nonnull
    private byte[] renderRaster(@Nonnull byte[] source) throws IOException {
        BufferedImage sourceImage;
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
            if (imageInput == null) {
                throw new IOException("Unsupported raster image format");
            }

            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported raster image format");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                var sourceWidth = reader.getWidth(0);
                var sourceHeight = reader.getHeight(0);
                if ((long) sourceWidth * sourceHeight > MAX_SOURCE_PIXELS) {
                    throw new IOException("Image dimensions exceed the maximum size");
                }
                sourceImage = reader.read(0);
            } finally {
                reader.dispose();
            }
        }

        var scale = Math.min(
                1d,
                Math.min(
                        (double) EMAIL_LOGO_MAX_WIDTH / sourceImage.getWidth(),
                        (double) EMAIL_LOGO_MAX_HEIGHT / sourceImage.getHeight()
                )
        );
        var width = Math.max(1, (int) Math.round(sourceImage.getWidth() * scale));
        var height = Math.max(1, (int) Math.round(sourceImage.getHeight() * scale));
        var targetImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(sourceImage, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        try (var output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(targetImage, "png", output)) {
                throw new IOException("No PNG writer is available");
            }
            return output.toByteArray();
        }
    }

    @Nonnull
    private byte[] addEmailLogoSurface(@Nonnull byte[] normalizedLogo) throws IOException {
        var logo = ImageIO.read(new ByteArrayInputStream(normalizedLogo));
        if (logo == null) {
            throw new IOException("Could not decode the normalized logo");
        }

        var availableWidth = EMAIL_LOGO_MAX_WIDTH - (2 * EMAIL_LOGO_PADDING);
        var availableHeight = EMAIL_LOGO_MAX_HEIGHT - (2 * EMAIL_LOGO_PADDING);
        var scale = Math.min(
                (double) availableWidth / logo.getWidth(),
                (double) availableHeight / logo.getHeight()
        );
        var logoWidth = Math.max(1, (int) Math.round(logo.getWidth() * scale));
        var logoHeight = Math.max(1, (int) Math.round(logo.getHeight() * scale));
        var logoX = (EMAIL_LOGO_MAX_WIDTH - logoWidth) / 2;
        var logoY = (EMAIL_LOGO_MAX_HEIGHT - logoHeight) / 2;

        var result = new BufferedImage(EMAIL_LOGO_MAX_WIDTH, EMAIL_LOGO_MAX_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(EMAIL_LOGO_BACKGROUND);
            graphics.fillRoundRect(
                    0,
                    0,
                    EMAIL_LOGO_MAX_WIDTH,
                    EMAIL_LOGO_MAX_HEIGHT,
                    EMAIL_LOGO_CORNER_DIAMETER,
                    EMAIL_LOGO_CORNER_DIAMETER
            );

            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(logo, logoX, logoY, logoWidth, logoHeight, null);
        } finally {
            graphics.dispose();
        }

        try (var output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(result, "png", output)) {
                throw new IOException("No PNG writer is available");
            }
            return output.toByteArray();
        }
    }

    public record SenderLogo(byte[] bytes, String contentType, String filename) {
    }

    private record CacheKey(UUID assetKey, Instant updated) {
    }
}
