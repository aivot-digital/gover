package de.aivot.GoverBackend.process.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Entity
@Table(name = "process_instance_attachments")
public class ProcessInstanceAttachmentEntity {
    @Id
    @Nonnull
    @Column(name = "key")
    private UUID key;

    @Nonnull
    @NotNull(message = "Der Dateiname darf nicht null sein.")
    @NotBlank(message = "Der Dateiname darf nicht leer sein sein.")
    @Size(max = 255, message = "Der Dateiname darf maximal 255 Zeichen lang sein.")
    private String fileName;

    @Nonnull
    @NotNull(message = "Die ID der Prozessinstanz darf nicht null sein.")
    private Long processInstanceId;

    @Nullable
    private Long processInstanceTaskId;

    @Nonnull
    @NotNull(message = "Die ID des Speicherindex-Items darf nicht null sein.")
    private Integer storageProviderId;

    @Nonnull
    @NotNull(message = "Der Pfad des Speicherindex-Items darf nicht null sein.")
    @NotBlank(message = "Der Pfad des Speicherindex-Items darf nicht leer sein sein.")
    private String storagePathFromRoot;

    @Nullable
    private String uploadedByUserId;

    /**
     * Ignore this field in database mapping and JSON serialization. This is only used when creating the file and saving it to the database.
     */
    @Transient
    @JsonIgnore
    private byte[] fileBytes;

    // region Constructors

    // Empty constructor is required by JPA
    public ProcessInstanceAttachmentEntity() {
    }

    // Full constructor for easier creation of the entity
    public ProcessInstanceAttachmentEntity(@Nonnull UUID key,
                                           @Nonnull String fileName,
                                           @Nonnull Long processInstanceId,
                                           @Nullable Long processInstanceTaskId,
                                           @Nonnull Integer storageProviderId,
                                           @Nonnull String storagePathFromRoot,
                                           @Nullable String uploadedByUserId,
                                           byte[] fileBytes) {
        this.key = key;
        this.fileName = fileName;
        this.processInstanceId = processInstanceId;
        this.processInstanceTaskId = processInstanceTaskId;
        this.storageProviderId = storageProviderId;
        this.storagePathFromRoot = storagePathFromRoot;
        this.uploadedByUserId = uploadedByUserId;
        this.fileBytes = fileBytes;
    }

    public static ProcessInstanceAttachmentEntity of(
            @Nonnull String fileName,
            @Nonnull Long processInstanceId,
            @Nullable Long processInstanceTaskId,
            @Nonnull byte[] fileBytes
    ) {
        return new ProcessInstanceAttachmentEntity(
                null,
                fileName,
                processInstanceId,
                processInstanceTaskId,
                null,
                null,
                null,
                fileBytes
        );
    }

    // endregion

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
    public String getFileName() {
        return fileName;
    }

    public ProcessInstanceAttachmentEntity setFileName(@Nonnull String fileName) {
        this.fileName = fileName;
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
    public Integer getStorageProviderId() {
        return storageProviderId;
    }

    public ProcessInstanceAttachmentEntity setStorageProviderId(@Nonnull Integer storageProviderId) {
        this.storageProviderId = storageProviderId;
        return this;
    }

    @Nonnull
    public String getStoragePathFromRoot() {
        return storagePathFromRoot;
    }

    public ProcessInstanceAttachmentEntity setStoragePathFromRoot(@Nonnull String storagePathFromRoot) {
        this.storagePathFromRoot = storagePathFromRoot;
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

    public byte[] getFileBytes() {
        return fileBytes;
    }

    public ProcessInstanceAttachmentEntity setFileBytes(byte[] fileBytes) {
        this.fileBytes = fileBytes;
        return this;
    }

    // endregion
}