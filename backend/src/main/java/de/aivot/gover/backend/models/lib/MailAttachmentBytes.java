package de.aivot.gover.backend.models.lib;

import org.springframework.http.MediaType;

public record MailAttachmentBytes(String filename, MediaType contentType, byte[] bytes) {
}
