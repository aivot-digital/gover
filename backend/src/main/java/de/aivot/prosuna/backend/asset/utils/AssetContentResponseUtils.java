package de.aivot.prosuna.backend.asset.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Builds the responses that stream asset content to browsers.
 * <p>
 * Staff members with ordinary permissions can upload assets with any file type, and the media type is derived from
 * the file extension. Browsers must therefore never interpret active content such as HTML, SVG or XML from an asset
 * on the application origin, where it would act with the session of whoever opens the link.
 */
public final class AssetContentResponseUtils {
    /**
     * Content that browsers display without running code. Matches the inline media types of process attachments.
     */
    private static final Set<MediaType> INLINE_MEDIA_TYPES = Set.of(
            MediaType.APPLICATION_PDF,
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF,
            new MediaType("image", "webp"),
            MediaType.TEXT_PLAIN
    );

    /**
     * Applied to all other content. A document rendered under this policy gets an opaque origin and cannot run
     * scripts, even if a browser displays it despite the attachment disposition.
     */
    private static final String ACTIVE_CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

    private AssetContentResponseUtils() {
    }

    @Nonnull
    public static ResponseEntity<InputStreamResource> createContentResponse(@Nullable String mimeType,
                                                                            @Nonnull String filename,
                                                                            boolean download,
                                                                            @Nonnull InputStream content) {
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        var inlineAllowed = INLINE_MEDIA_TYPES.contains(new MediaType(mediaType.getType(), mediaType.getSubtype()));
        var contentDisposition = ContentDisposition
                .builder(download || !inlineAllowed ? "attachment" : "inline")
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        // Unlike process attachments, active content keeps its media type: the application embeds public assets such
        // as SVG logos and icons in image elements, and browsers render SVG images only with their own media type.
        // The attachment disposition and the sandbox policy prevent the browser from displaying the asset as a page.
        var responseBuilder = ResponseEntity
                .ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        if (!inlineAllowed) {
            responseBuilder.header("Content-Security-Policy", ACTIVE_CONTENT_SECURITY_POLICY);
        }

        return responseBuilder.body(new InputStreamResource(content));
    }
}
