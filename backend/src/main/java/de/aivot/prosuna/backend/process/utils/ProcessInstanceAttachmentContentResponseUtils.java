package de.aivot.prosuna.backend.process.utils;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAttachmentEntity;
import jakarta.annotation.Nonnull;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Builds the responses that stream process instance attachments to staff members and customers.
 * <p>
 * Attachments can originate from external senders, for example through webhooks or FIT-Connect. Browsers must
 * therefore never interpret active content such as HTML, SVG or XML on the application origin.
 */
public final class ProcessInstanceAttachmentContentResponseUtils {
    /**
     * Media types that browsers display without executing active content. All other attachments are served as
     * downloads with a generic media type.
     */
    private static final Set<MediaType> INLINE_MEDIA_TYPES = Set.of(
            MediaType.APPLICATION_PDF,
            MediaType.IMAGE_PNG,
            MediaType.IMAGE_JPEG,
            MediaType.IMAGE_GIF,
            new MediaType("image", "webp"),
            MediaType.TEXT_PLAIN
    );

    private ProcessInstanceAttachmentContentResponseUtils() {
    }

    @Nonnull
    public static ResponseEntity<InputStreamResource> createContentResponse(@Nonnull ProcessInstanceAttachmentEntity attachment,
                                                                            boolean download,
                                                                            @Nonnull InputStream content) {
        MediaType mediaType;
        try {
            var mimeType = URLConnection.guessContentTypeFromName(attachment.getFileName());
            mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (InvalidMediaTypeException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        var inlineAllowed = INLINE_MEDIA_TYPES.contains(new MediaType(mediaType.getType(), mediaType.getSubtype()));
        if (!inlineAllowed) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        var contentDisposition = ContentDisposition
                .builder(download || !inlineAllowed ? "attachment" : "inline")
                .filename(attachment.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(new InputStreamResource(content));
    }
}
