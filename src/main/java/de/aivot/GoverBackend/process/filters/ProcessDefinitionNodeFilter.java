package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessDefinitionNodeFilter implements Filter<ProcessNodeEntity> {
    private Integer id;
    private Integer processDefinitionId;
    private Integer processDefinitionVersion;
    private String dataKey;
    private String codeKey;

    public static ProcessDefinitionNodeFilter create() {
        return new ProcessDefinitionNodeFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessNodeEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withEquals("id", id)
                .withEquals("processId", processDefinitionId)
                .withEquals("processVersion", processDefinitionVersion)
                .withContains("dataKey", dataKey)
                .withContains("codeKey", codeKey);

        return builder.build();
    }

    public Integer getId() {
        return id;
    }

    public ProcessDefinitionNodeFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionNodeFilter setProcessDefinitionId(Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionNodeFilter setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    public String getDataKey() {
        return dataKey;
    }

    public ProcessDefinitionNodeFilter setDataKey(String dataKey) {
        this.dataKey = dataKey;
        return this;
    }

    public String getCodeKey() {
        return codeKey;
    }

    public ProcessDefinitionNodeFilter setCodeKey(String codeKey) {
        this.codeKey = codeKey;
        return this;
    }
}

