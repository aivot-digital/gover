package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessVersionFilter implements Filter<ProcessVersionEntity> {
    private Integer processId;
    private Integer processVersion;
    private ProcessVersionStatus status;

    public static ProcessVersionFilter create() {
        return new ProcessVersionFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessVersionEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessVersionEntity.class)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion)
                .withEquals("status", status);

        return builder.build();
    }

    public Integer getProcessId() {
        return processId;
    }

    public ProcessVersionFilter setProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessVersionFilter setProcessVersion(Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    public ProcessVersionStatus getStatus() {
        return status;
    }

    public ProcessVersionFilter setStatus(ProcessVersionStatus status) {
        this.status = status;
        return this;
    }
}

