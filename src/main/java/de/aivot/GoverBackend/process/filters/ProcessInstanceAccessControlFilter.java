package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

public class ProcessInstanceAccessControlFilter implements Filter<ProcessInstanceAccessControlEntity> {
    private Integer sourceTeamId;
    private Integer sourceDepartmentId;
    private Integer targetProcessInstanceId;
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

    public Integer getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public ProcessInstanceAccessControlFilter setTargetProcessInstanceId(Integer targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
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

