package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ProcessInstanceTaskFilter implements Filter<ProcessInstanceTaskEntity> {
    private UUID accessKey;
    private Long processInstanceId;
    private Integer processId;
    private Integer processVersion;
    private Integer processNodeId;
    private String assignedUserId;
    private ProcessTaskStatus status;

    public static ProcessInstanceTaskFilter create() {
        return new ProcessInstanceTaskFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceTaskEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceTaskEntity.class)
                .withEquals("accessKey", accessKey)
                .withEquals("processInstanceId", processInstanceId)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion)
                .withEquals("processNodeId", processNodeId)
                .withEquals("status", status)
                .withContains("assignedUserId", assignedUserId);

        return builder.build();
    }

    public UUID getAccessKey() {
        return accessKey;
    }

    public ProcessInstanceTaskFilter setAccessKey(UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public Long getProcessInstanceId() {
        return processInstanceId;
    }

    public ProcessInstanceTaskFilter setProcessInstanceId(Long processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    public Integer getProcessId() {
        return processId;
    }

    public ProcessInstanceTaskFilter setProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessInstanceTaskFilter setProcessVersion(Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    public Integer getProcessNodeId() {
        return processNodeId;
    }

    public ProcessInstanceTaskFilter setProcessNodeId(Integer processNodeId) {
        this.processNodeId = processNodeId;
        return this;
    }

    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessInstanceTaskFilter setAssignedUserId(String assignedUserId) {
        this.assignedUserId = assignedUserId;
        return this;
    }

    public ProcessTaskStatus getStatus() {
        return status;
    }

    public ProcessInstanceTaskFilter setStatus(ProcessTaskStatus status) {
        this.status = status;
        return this;
    }
}
