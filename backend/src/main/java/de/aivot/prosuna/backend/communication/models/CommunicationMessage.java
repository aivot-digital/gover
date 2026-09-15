package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
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
        List<CommunicationMessageAttachment> attachments,
        @Nullable
        UserEntity sendingUser,
        @Nullable
        DepartmentEntity sendingDepartment
) {
    public CommunicationMessage {
        callToActions = callToActions == null ? List.of() : callToActions;
        attachments = attachments == null ? List.of() : attachments;
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
        return new CommunicationMessage(subject, body, htmlBody, List.of(), Instant.now(), attachments, null, null);
    }

    public static CommunicationMessage of(
            @Nonnull String subject,
            @Nonnull String body,
            @Nonnull String htmlBody,
            @Nonnull List<CommunicationMessageCallToAction> callToActions,
            @Nonnull List<CommunicationMessageAttachment> attachments
    ) {
        return new CommunicationMessage(subject, body, htmlBody, callToActions, Instant.now(), attachments, null, null);
    }

    @Nonnull
    public CommunicationMessage withSendingContext(
            @Nullable UserEntity sendingUser,
            @Nullable DepartmentEntity sendingDepartment
    ) {
        return new CommunicationMessage(
                subject,
                body,
                htmlBody,
                callToActions,
                timestamp,
                attachments,
                sendingUser,
                sendingDepartment
        );
    }
}
