package de.aivot.prosuna.backend.communication.models;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;

public record CommunicationMessage(
        @Nonnull
        String subject,
        // Plain text version of markdownBody, for example for the text part of mails.
        @Nonnull
        String body,
        // Providers render it with MarkdownRenderer, which escapes raw HTML.
        @Nonnull
        String markdownBody,
        @Nonnull
        List<CommunicationMessageCallToAction> callToActions,
        @Nonnull
        Instant timestamp,
        @Nonnull
        List<CommunicationMessageAttachment> attachments,
        @Nullable
        UserEntity sendingUser,
        @Nullable
        DepartmentEntity sendingDepartment,
        @Nullable
        VDepartmentShadowedEntity signatureDepartment,
        @Nullable
        String referenceId
) {
    public static class Builder {
        private String subject;
        private String body;
        private String markdownBody;
        private List<CommunicationMessageCallToAction> callToActions = List.of();
        private Instant timestamp = Instant.now();
        private List<CommunicationMessageAttachment> attachments = List.of();
        private UserEntity sendingUser;
        private DepartmentEntity sendingDepartment;
        private VDepartmentShadowedEntity signatureDepartment;
        private String referenceId;

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder markdownBody(String markdownBody) {
            this.markdownBody = markdownBody;
            return this;
        }

        public Builder callToActions(List<CommunicationMessageCallToAction> callToActions) {
            this.callToActions = callToActions;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder attachments(List<CommunicationMessageAttachment> attachments) {
            this.attachments = attachments;
            return this;
        }

        public Builder sendingUser(UserEntity sendingUser) {
            this.sendingUser = sendingUser;
            return this;
        }

        public Builder sendingDepartment(DepartmentEntity sendingDepartment) {
            this.sendingDepartment = sendingDepartment;
            return this;
        }

        public Builder signatureDepartment(VDepartmentShadowedEntity signatureDepartment) {
            this.signatureDepartment = signatureDepartment;
            return this;
        }

        public Builder referenceId(String referenceId) {
            this.referenceId = referenceId;
            return this;
        }

        public CommunicationMessage build() {
            assert subject != null : "Subject must not be null";
            assert body != null : "Body must not be null";
            assert markdownBody != null : "Markdown body must not be null";

            return new CommunicationMessage(
                    subject,
                    body,
                    markdownBody,
                    callToActions != null ? callToActions : List.of(),
                    timestamp,
                    attachments != null ? attachments : List.of(),
                    sendingUser,
                    sendingDepartment,
                    signatureDepartment,
                    referenceId
            );
        }
    }
}
