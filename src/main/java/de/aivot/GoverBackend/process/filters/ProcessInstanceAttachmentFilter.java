package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAttachmentEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

public class ProcessInstanceAttachmentFilter implements Filter<ProcessInstanceAttachmentEntity> {
    private Long processInstanceId;
    private Long processInstanceTaskId;
    private String fileName;
    private String mimeType;
    private String uploadedByUserId;

    public static ProcessInstanceAttachmentFilter create() {
        return new ProcessInstanceAttachmentFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceAttachmentEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceAttachmentEntity.class)
                .withEquals("processInstanceId", processInstanceId)
                .withEquals("processInstanceTaskId", processInstanceTaskId)
                .withContains("fileName", fileName)
                .withEquals("mimeType", mimeType)
                .withEquals("uploadedByUserId", uploadedByUserId);

        return builder.build();
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

    public String getFileName() {
        return fileName;
    }

    public ProcessInstanceAttachmentFilter setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ProcessInstanceAttachmentFilter setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public ProcessInstanceAttachmentFilter setUploadedByUserId(String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
        return this;
    }
}

