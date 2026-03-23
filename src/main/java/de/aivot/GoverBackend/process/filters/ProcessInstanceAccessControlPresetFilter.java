package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

public class ProcessInstanceAccessControlPresetFilter implements Filter<ProcessInstanceAccessControlPresetEntity> {
    private Integer sourceTeamId;
    private Integer sourceDepartmentId;
    private Integer targetProcessId;

    public static ProcessInstanceAccessControlPresetFilter create() {
        return new ProcessInstanceAccessControlPresetFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessInstanceAccessControlPresetEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceAccessControlPresetEntity.class)
                .withEquals("sourceTeamId", sourceTeamId)
                .withEquals("sourceDepartmentId", sourceDepartmentId)
                .withEquals("targetProcessId", targetProcessId);

        return builder.build();
    }

    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ProcessInstanceAccessControlPresetFilter setSourceTeamId(Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    public Integer getSourceDepartmentId() {
        return sourceDepartmentId;
    }

    public ProcessInstanceAccessControlPresetFilter setSourceDepartmentId(Integer sourceDepartmentId) {
        this.sourceDepartmentId = sourceDepartmentId;
        return this;
    }

    public Integer getTargetProcessId() {
        return targetProcessId;
    }

    public ProcessInstanceAccessControlPresetFilter setTargetProcessId(Integer targetProcessId) {
        this.targetProcessId = targetProcessId;
        return this;
    }
}

