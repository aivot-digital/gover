package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessInstanceAttachmentFilter implements Filter<ProcessInstanceAttachmentEntity> {
    private UUID key;
    private Long processInstanceId;
    private Long processInstanceTaskId;
    private String uploadedByUserId;
    private LocalDateTime uploadedAt;

    public static ProcessInstanceAttachmentFilter create() {
        return new ProcessInstanceAttachmentFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceAttachmentEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceAttachmentEntity.class)
                .withEquals("key", key)
                .withEquals("processInstanceId", processInstanceId)
                .withEquals("processInstanceTaskId", processInstanceTaskId)
                .withContains("uploadedByUserId", uploadedByUserId)
                .withEquals("uploadedAt", uploadedAt);

        return builder.build();
    }

    public UUID getKey() {
        return key;
    }

    public ProcessInstanceAttachmentFilter setKey(UUID key) {
        this.key = key;
        return this;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceAttachmentFilter setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public Long getProcessInstanceTaskId() {
        return processInstanceTaskId;
    }

    public ProcessInstanceAttachmentFilter setProcessInstanceTaskId(Long processInstanceTaskId) {
        this.processInstanceTaskId = processInstanceTaskId;
        return this;
    }

    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public ProcessInstanceAttachmentFilter setUploadedByUserId(String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
        return this;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public ProcessInstanceAttachmentFilter setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
        return this;
    }
}

