package de.aivot.prosuna.backend.communication.models;

import java.io.InputStream;

public interface CommunicationMessageAttachment {
    String getName();
    String getContentType();
    InputStream getContent();
}
