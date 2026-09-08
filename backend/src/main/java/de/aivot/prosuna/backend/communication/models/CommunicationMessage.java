package de.aivot.prosuna.backend.communication.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

public record CommunicationMessage(
        @Nonnull
        String subject,
        @Nonnull
        String body,
        @Nonnull
        String htmlBody,
        @Nonnull
        List<CommunicationMessageCallToAction> callToActions,
        @Nonnull
        Instant timestamp,
        @Nonnull
        List<CommunicationMessageAttachment> attachments
) {
    public CommunicationMessage {
        callToActions = callToActions == null ? List.of() : callToActions;
        attachments = attachments == null ? List.of() : attachments;
    }

    /**
     * Keeps callers using the message shape from before call-to-actions were introduced compatible.
     */
    public CommunicationMessage(
            @Nonnull String subject,
            @Nonnull String body,
            @Nonnull String htmlBody,
            @Nonnull Instant timestamp,
            @Nullable List<CommunicationMessageAttachment> attachments
    ) {
        this(subject, body, htmlBody, List.of(), timestamp, attachments);
    }

    public static CommunicationMessage of(
            @Nonnull String subject,
            @Nonnull String body,
            @Nonnull String htmlBody
    ) {
        return of(subject, body, htmlBody, List.of());
    }

    public static CommunicationMessage of(
            @Nonnull String subject,
            @Nonnull String body,
            @Nonnull String htmlBody,
            @Nonnull List<CommunicationMessageAttachment> attachments
    ) {
        return new CommunicationMessage(subject, body, htmlBody, List.of(), Instant.now(), attachments);
    }

    public static CommunicationMessage of(
            @Nonnull String subject,
            @Nonnull String body,
            @Nonnull String htmlBody,
            @Nonnull List<CommunicationMessageCallToAction> callToActions,
            @Nonnull List<CommunicationMessageAttachment> attachments
    ) {
        return new CommunicationMessage(subject, body, htmlBody, callToActions, Instant.now(), attachments);
    }
}
