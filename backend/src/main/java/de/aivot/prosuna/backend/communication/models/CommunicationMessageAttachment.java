package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nullable;

import java.io.InputStream;

public interface CommunicationMessageAttachment {
    @Nullable
    String getName();

    @Nullable
    String getContentType();

    @Nullable
    InputStream getContent();
}
