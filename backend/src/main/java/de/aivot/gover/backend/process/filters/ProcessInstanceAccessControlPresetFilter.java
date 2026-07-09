package de.aivot.gover.backend.process.filters;

import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.process.entities.ProcessInstanceAccessControlPresetEntity;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ProcessInstanceAccessControlPresetFilter implements Filter<ProcessInstanceAccessControlPresetEntity> {
    private Integer sourceTeamId;
    private Integer sourceDepartmentId;
    private Integer targetProcessId;
    private List<Integer> targetProcessIds;
    private Integer targetProcessVersion;

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
                .withEquals("targetProcessId", targetProcessId)
                .withInList("targetProcessId", targetProcessIds)
                .withEquals("targetProcessVersion", targetProcessVersion);

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

    public List<Integer> getTargetProcessIds() {
        return targetProcessIds;
    }

    public ProcessInstanceAccessControlPresetFilter setTargetProcessIds(List<Integer> targetProcessIds) {
        this.targetProcessIds = targetProcessIds;
        return this;
    }

    public ProcessInstanceAccessControlPresetFilter setTargetProcessVersion(Integer targetProcessVersion) {
        this.targetProcessVersion = targetProcessVersion;
        return this;
    }
}
