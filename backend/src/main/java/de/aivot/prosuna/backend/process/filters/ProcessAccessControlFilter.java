package de.aivot.prosuna.backend.process.filters;

import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.process.entities.ProcessAccessControlEntity;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProcessAccessControlFilter implements Filter<ProcessAccessControlEntity> {
    private Integer sourceTeamId;
    private Integer sourceDepartmentId;
    private Integer targetProcessId;
    private List<Integer> targetProcessIds;

    public static ProcessAccessControlFilter create() {
        return new ProcessAccessControlFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessAccessControlEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessAccessControlEntity.class)
                .withEquals("sourceTeamId", sourceTeamId)
                .withEquals("sourceDepartmentId", sourceDepartmentId)
                .withEquals("targetProcessId", targetProcessId)
                .withInList("targetProcessId", targetProcessIds);

        return builder.build();
    }

    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ProcessAccessControlFilter setSourceTeamId(Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    public Integer getSourceDepartmentId() {
        return sourceDepartmentId;
    }

    public ProcessAccessControlFilter setSourceDepartmentId(Integer sourceDepartmentId) {
        this.sourceDepartmentId = sourceDepartmentId;
        return this;
    }

    public Integer getTargetProcessId() {
        return targetProcessId;
    }

    public ProcessAccessControlFilter setTargetProcessId(Integer targetProcessId) {
        this.targetProcessId = targetProcessId;
        return this;
    }

    public List<Integer> getTargetProcessIds() {
        return targetProcessIds;
    }

    public ProcessAccessControlFilter setTargetProcessIds(List<Integer> targetProcessIds) {
        this.targetProcessIds = targetProcessIds;
        return this;
    }
}
