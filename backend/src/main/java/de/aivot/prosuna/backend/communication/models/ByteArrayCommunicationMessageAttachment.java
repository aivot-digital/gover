package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/** Repeatable in-memory attachment used at the synchronous v1 dispatch boundary. */
public record ByteArrayCommunicationMessageAttachment(
        @Nonnull String name,
        @Nullable String contentType,
        @Nonnull byte[] bytes
) implements CommunicationMessageAttachment {
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(bytes);
    }
}
