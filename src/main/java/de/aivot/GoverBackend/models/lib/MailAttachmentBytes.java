package de.aivot.GoverBackend.models.lib;

import org.springframework.http.MediaType;

public record MailAttachmentBytes(String filename, MediaType contentType, byte[] bytes) {
}
