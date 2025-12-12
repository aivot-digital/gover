package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessInstanceFilter implements Filter<ProcessInstanceEntity> {
    private Long id;
    private UUID accessKey;
    private Integer processDefinitionId;
    private Integer processDefinitionVersion;
    private ProcessInstanceStatus status;
    private ProcessInstanceStatus statusIsNot;
    private String statusOverride;
    private String assignedFileNumber;
    private String tag;

    public static ProcessInstanceFilter create() {
        return new ProcessInstanceFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceEntity.class)
                .withEquals("id", id)
                .withEquals("accessKey", accessKey)
                .withEquals("processDefinitionId", processDefinitionId)
                .withEquals("processDefinitionVersion", processDefinitionVersion)
                .withEquals("status", status)
                .withNotEquals("status", statusIsNot)
                .withEquals("statusOverride", statusOverride)
                .withContains("assignedFileNumbers", assignedFileNumber) // TODO: introduce in array filter
                .withContains("tags", tag); // TODO: introduce in array filter

        return builder.build();
    }

    public Long getId() {
        return id;
    }

    public ProcessInstanceFilter setId(Long id) {
        this.id = id;
        return this;
    }

    public UUID getAccessKey() {
        return accessKey;
    }

    public ProcessInstanceFilter setAccessKey(UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessInstanceFilter setProcessDefinitionId(Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessInstanceFilter setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    public ProcessInstanceStatus getStatus() {
        return status;
    }

    public ProcessInstanceFilter setStatus(ProcessInstanceStatus status) {
        this.status = status;
        return this;
    }

    public ProcessInstanceStatus getStatusIsNot() {
        return statusIsNot;
    }

    public ProcessInstanceFilter setStatusIsNot(ProcessInstanceStatus statusIsNot) {
        this.statusIsNot = statusIsNot;
        return this;
    }

    public String getStatusOverride() {
        return statusOverride;
    }

    public ProcessInstanceFilter setStatusOverride(String statusOverride) {
        this.statusOverride = statusOverride;
        return this;
    }

    public String getAssignedFileNumber() {
        return assignedFileNumber;
    }

    public ProcessInstanceFilter setAssignedFileNumber(String assignedFileNumber) {
        this.assignedFileNumber = assignedFileNumber;
        return this;
    }

    public String getTag() {
        return tag;
    }

    public ProcessInstanceFilter setTag(String tag) {
        this.tag = tag;
        return this;
    }
}

