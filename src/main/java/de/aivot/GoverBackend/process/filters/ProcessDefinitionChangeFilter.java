package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessChangeEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;

public class ProcessDefinitionChangeFilter implements Filter<ProcessChangeEntity> {
    private Long id;
    private LocalDateTime timestamp;
    private String userId;
    private Integer processDefinitionId;
    private Integer processDefinitionVersion;
    private Integer processDefinitionNodeId;
    private Integer processDefinitionEdgeId;
    private Short changeType;
    private String comment;

    public static ProcessDefinitionChangeFilter create() {
        return new ProcessDefinitionChangeFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessChangeEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessChangeEntity.class)
                .withEquals("id", id)
                .withEquals("timestamp", timestamp)
                .withContains("userId", userId)
                .withEquals("processId", processDefinitionId)
                .withEquals("processVersion", processDefinitionVersion)
                .withEquals("processNodeId", processDefinitionNodeId)
                .withEquals("processEdgeId", processDefinitionEdgeId)
                .withEquals("changeType", changeType)
                .withContains("comment", comment);

        return builder.build();
    }

    public Long getId() {
        return id;
    }

    public ProcessDefinitionChangeFilter setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public ProcessDefinitionChangeFilter setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public ProcessDefinitionChangeFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionChangeFilter setProcessDefinitionId(Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionChangeFilter setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    public Integer getProcessDefinitionNodeId() {
        return processDefinitionNodeId;
    }

    public ProcessDefinitionChangeFilter setProcessDefinitionNodeId(Integer processDefinitionNodeId) {
        this.processDefinitionNodeId = processDefinitionNodeId;
        return this;
    }

    public Integer getProcessDefinitionEdgeId() {
        return processDefinitionEdgeId;
    }

    public ProcessDefinitionChangeFilter setProcessDefinitionEdgeId(Integer processDefinitionEdgeId) {
        this.processDefinitionEdgeId = processDefinitionEdgeId;
        return this;
    }

    public Short getChangeType() {
        return changeType;
    }

    public ProcessDefinitionChangeFilter setChangeType(Short changeType) {
        this.changeType = changeType;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ProcessDefinitionChangeFilter setComment(String comment) {
        this.comment = comment;
        return this;
    }
}

