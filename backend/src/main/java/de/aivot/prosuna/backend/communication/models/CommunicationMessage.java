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
        String htmlBody,
        @Nullable
        Instant timestamp,
        @Nullable
        List<CommunicationMessageAttachment> attachments
) {
    public static CommunicationMessage of(
            @Nullable String subject,
            @Nullable String body,
            @Nullable String htmlBody
    ) {
        return of(subject, body, htmlBody, List.of());
    }

    public static CommunicationMessage of(
            @Nullable String subject,
            @Nullable String body,
            @Nullable String htmlBody,
            @Nullable List<CommunicationMessageAttachment> attachments
    ) {
        return new CommunicationMessage(subject, body, htmlBody, Instant.now(), attachments);
    }
}
