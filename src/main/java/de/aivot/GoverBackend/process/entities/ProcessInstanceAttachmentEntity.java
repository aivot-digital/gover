package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "process_instance_attachments")
public class ProcessInstanceAttachmentEntity {
    @Id
    @Nonnull
    @Column(name = "key")
    private UUID key;

    @Nonnull
    @NotNull(message = "Die ID der Prozessinstanz darf nicht null sein.")
    private Long processInstanceId;

    @Nullable
    private Long processInstanceTaskId;

    @Nullable
    private String uploadedByUserId;

    @NotNull(message = "Das Hochladedatum darf nicht null sein.")
    private LocalDateTime uploadedAt;

    // region Getters and Setters

    @Nonnull
    public UUID getKey() {
        return key;
    }

    public ProcessInstanceAttachmentEntity setKey(@Nonnull UUID key) {
        this.key = key;
        return this;
    }

    @Nonnull
    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceAttachmentEntity setProcessInstanceId(@Nonnull Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Nullable
    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceAttachmentEntity setProcessInstanceTaskId(@Nullable Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    @Nullable
    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public ProcessInstanceAttachmentEntity setUploadedByUserId(@Nullable String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
        return this;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public ProcessInstanceAttachmentEntity setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
        return this;
    }


    // endregion
}