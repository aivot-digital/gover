package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

public class ProcessTestConfigFilter implements Filter<ProcessEntity> {
    private String name;
    private Integer processId;
    private Integer processVersion;

    public static ProcessTestConfigFilter create() {
        return new ProcessTestConfigFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessEntity.class)
                .withContains("name", name)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion);

        return builder.build();
    }

    public String getName() {
        return name;
    }

    public ProcessTestConfigFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getProcessId() {
        return processId;
    }

    public ProcessTestConfigFilter setProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessTestConfigFilter setProcessVersion(Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }
}

