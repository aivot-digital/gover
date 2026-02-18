package de.aivot.GoverBackend.communication.models;

import java.time.LocalDateTime;
import java.util.List;

public interface CommunicationMessage {
    LocalDateTime getTimestamp();
    List<CommunicationMessageAttachment> getAttachments();
    void setAttachments(List<CommunicationMessageAttachment> attachments);
}
