package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

public record CommunicationMessage(
        @Nullable
        String subject,
        @Nullable
        String body,
        @Nullable
        Instant timestamp,
        @Nullable
        List<CommunicationMessageAttachment> attachments
) {
}
