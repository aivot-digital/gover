package de.aivot.prosuna.backend.process.filters;

import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProcessInstanceAccessControlFilter implements Filter<ProcessInstanceAccessControlEntity> {
    private Integer sourceTeamId;
    private Integer sourceDepartmentId;
    private Long targetProcessInstanceId;
    private List<Long> targetProcessInstanceIds;
    private Integer targetProcessInstanceTaskId;

    public static ProcessInstanceAccessControlFilter create() {
        return new ProcessInstanceAccessControlFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceAccessControlEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceAccessControlEntity.class)
                .withEquals("sourceTeamId", sourceTeamId)
                .withEquals("sourceDepartmentId", sourceDepartmentId)
                .withEquals("targetProcessInstanceId", targetProcessInstanceId)
                .withInList("targetProcessInstanceId", targetProcessInstanceIds)
                .withEquals("targetProcessInstanceTaskId", targetProcessInstanceTaskId);

        return builder.build();
    }

    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ProcessInstanceAccessControlFilter setSourceTeamId(Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    public Integer getSourceDepartmentId() {
        return sourceDepartmentId;
    }

    public ProcessInstanceAccessControlFilter setSourceDepartmentId(Integer sourceDepartmentId) {
        this.sourceDepartmentId = sourceDepartmentId;
        return this;
    }

    public Long getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public ProcessInstanceAccessControlFilter setTargetProcessInstanceId(Long targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
        return this;
    }

    public List<Long> getTargetProcessInstanceIds() {
        return targetProcessInstanceIds;
    }

    public ProcessInstanceAccessControlFilter setTargetProcessInstanceIds(List<Long> targetProcessInstanceIds) {
        this.targetProcessInstanceIds = targetProcessInstanceIds;
        return this;
    }

    public Integer getTargetProcessInstanceTaskId() {
        return targetProcessInstanceTaskId;
    }

    public ProcessInstanceAccessControlFilter setTargetProcessInstanceTaskId(Integer targetProcessInstanceTaskId) {
        this.targetProcessInstanceTaskId = targetProcessInstanceTaskId;
        return this;
    }
}
