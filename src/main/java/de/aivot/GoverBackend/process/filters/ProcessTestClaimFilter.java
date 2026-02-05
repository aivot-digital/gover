package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

public class ProcessTestClaimFilter implements Filter<ProcessTestClaimEntity> {
    private Integer processId;
    private Integer processVersion;
    private String owningUserId;

    public static ProcessTestClaimFilter create() {
        return new ProcessTestClaimFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessTestClaimEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessTestClaimEntity.class)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion)
                .withEquals("owningUserId", owningUserId);

        return builder.build();
    }

    public Integer getProcessId() {
        return processId;
    }

    public ProcessTestClaimFilter setProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessTestClaimFilter setProcessVersion(Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    public String getOwningUserId() {
        return owningUserId;
    }

    public ProcessTestClaimFilter setOwningUserId(String owningUserId) {
        this.owningUserId = owningUserId;
        return this;
    }
}

