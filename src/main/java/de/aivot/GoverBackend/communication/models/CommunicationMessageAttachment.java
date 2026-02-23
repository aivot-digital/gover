package de.aivot.GoverBackend.communication.models;

import java.io.InputStream;

public interface CommunicationMessageAttachment {
    String getName();
    String getContentType();
    InputStream getContent();
}
