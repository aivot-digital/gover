package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessNodeFilter implements Filter<ProcessNodeEntity> {
    private Integer id;
    private String name;
    private Integer processId;
    private Integer processVersion;
    private String dataKey;
    private String processNodeDefinitionKey;
    private String processNodeDefinitionVersion;

    public static ProcessNodeFilter create() {
        return new ProcessNodeFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessNodeEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withEquals("id", id)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion)
                .withContains("name", name)
                .withEquals("dataKey", dataKey)
                .withEquals("processNodeDefinitionKey", processNodeDefinitionKey)
                .withEquals("processNodeDefinitionVersion", processNodeDefinitionVersion);

        return builder.build();
    }

    public Integer getId() {
        return id;
    }

    public ProcessNodeFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ProcessNodeFilter setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getProcessId() {
        return processId;
    }

    public ProcessNodeFilter setProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    public Integer getProcessVersion() {
        return processVersion;
    }

    public ProcessNodeFilter setProcessVersion(Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    public String getDataKey() {
        return dataKey;
    }

    public ProcessNodeFilter setDataKey(String dataKey) {
        this.dataKey = dataKey;
        return this;
    }

    public String getProcessNodeDefinitionKey() {
        return processNodeDefinitionKey;
    }

    public ProcessNodeFilter setProcessNodeDefinitionKey(String processNodeDefinitionKey) {
        this.processNodeDefinitionKey = processNodeDefinitionKey;
        return this;
    }

    public String getProcessNodeDefinitionVersion() {
        return processNodeDefinitionVersion;
    }

    public ProcessNodeFilter setProcessNodeDefinitionVersion(String processNodeDefinitionVersion) {
        this.processNodeDefinitionVersion = processNodeDefinitionVersion;
        return this;
    }
}

