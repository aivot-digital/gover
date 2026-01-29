package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
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

    @Nonnull
    @NotNull(message = "Der Dateiname darf nicht null sein.")
    @NotBlank(message = "Der Dateiname darf nicht leer sein.")
    private String filename;

    @Nonnull
    @NotNull(message = "Der MIME-Typ darf nicht null sein.")
    @NotBlank(message = "Der MIME-Typ darf nicht leer sein.")
    private String mimeType;

    @Nonnull
    @NotNull(message = "Die Dateigröße darf nicht null sein.")
    private Long sizeBytes;

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

    @Nonnull
    public String getFilename() {
        return filename;
    }

    public ProcessInstanceAttachmentEntity setFilename(@Nonnull String filename) {
        this.filename = filename;
        return this;
    }

    @Nonnull
    public String getMimeType() {
        return mimeType;
    }

    public ProcessInstanceAttachmentEntity setMimeType(@Nonnull String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    @Nonnull
    public Long getSizeBytes() {
        return sizeBytes;
    }

    public ProcessInstanceAttachmentEntity setSizeBytes(@Nonnull Long sizeBytes) {
        this.sizeBytes = sizeBytes;
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