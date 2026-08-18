package de.aivot.gover.backend.process.filters;

import de.aivot.gover.backend.lib.models.EntityFilter;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.enums.ProcessTaskStatus;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.UUID;

public class ProcessInstanceTaskFilter extends EntityFilter<ProcessInstanceTaskEntity> {
    private String accessKey;
    private Long processInstanceId;
    private Integer processId;
    private Integer processVersion;
    private Integer processNodeId;
    private String assignedUserId;
    private ProcessTaskStatus status;
    private List<ProcessTaskStatus> anyStatus;

    public static ProcessInstanceTaskFilter create() {
        return new ProcessInstanceTaskFilter();
    }

    @Nonnull
    @Override
    public SpecificationBuilder<ProcessInstanceTaskEntity> createSpecBuilder() {
        return SpecificationBuilder
                .create(ProcessInstanceTaskEntity.class)
                .withEquals("accessKey", accessKey)
                .withEquals("processInstanceId", processInstanceId)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion)
                .withEquals("processNodeId", processNodeId)
                .withEquals("status", status)
                .withContains("assignedUserId", assignedUserId)
                .withInList("status", anyStatus);
    }

    public String getAccessKey() {
        return accessKey;
    }

    public ProcessInstanceTaskFilter setAccessKey(String accessKey) {
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

    public List<ProcessTaskStatus> getAnyStatus() {
        return anyStatus;
    }

    public ProcessInstanceTaskFilter setAnyStatus(List<ProcessTaskStatus> anyStatus) {
        this.anyStatus = anyStatus;
        return this;
    }
}
