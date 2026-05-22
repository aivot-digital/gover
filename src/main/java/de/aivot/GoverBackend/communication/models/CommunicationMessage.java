package de.aivot.GoverBackend.communication.models;

import java.time.Instant;
import java.util.List;

public interface CommunicationMessage {
    Instant getTimestamp();
    List<CommunicationMessageAttachment> getAttachments();
    void setAttachments(List<CommunicationMessageAttachment> attachments);
}
