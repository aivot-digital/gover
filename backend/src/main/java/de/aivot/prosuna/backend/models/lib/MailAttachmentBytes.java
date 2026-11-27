package de.aivot.prosuna.backend.models.lib;

import org.springframework.http.MediaType;

public record MailAttachmentBytes(String filename, MediaType contentType, byte[] bytes) {
}
