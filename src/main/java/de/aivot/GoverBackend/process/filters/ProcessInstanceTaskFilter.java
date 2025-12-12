package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessInstanceTaskFilter implements Filter<ProcessInstanceTaskEntity> {
    private Long id;
    private UUID accessKey;
    private Long processInstanceId;
    private Integer processDefinitionId;
    private Integer processDefinitionVersion;
    private Integer processDefinitionNodeId;
    private String assignedUserId;

    public static ProcessInstanceTaskFilter create() {
        return new ProcessInstanceTaskFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceTaskEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceTaskEntity.class)
                .withEquals("id", id)
                .withEquals("accessKey", accessKey)
                .withEquals("processInstanceId", processInstanceId)
                .withEquals("processDefinitionId", processDefinitionId)
                .withEquals("processDefinitionVersion", processDefinitionVersion)
                .withEquals("processDefinitionNodeId", processDefinitionNodeId)
                .withContains("assignedUserId", assignedUserId);

        return builder.build();
    }

    public Long getId() {
        return id;
    }

    public ProcessInstanceTaskFilter setId(Long id) {
        this.id = id;
        return this;
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

    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessInstanceTaskFilter setProcessDefinitionId(Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessInstanceTaskFilter setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    public Integer getProcessDefinitionNodeId() {
        return processDefinitionNodeId;
    }

    public ProcessInstanceTaskFilter setProcessDefinitionNodeId(Integer processDefinitionNodeId) {
        this.processDefinitionNodeId = processDefinitionNodeId;
        return this;
    }

    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessInstanceTaskFilter setAssignedUserId(String assignedUserId) {
        this.assignedUserId = assignedUserId;
        return this;
    }
}

