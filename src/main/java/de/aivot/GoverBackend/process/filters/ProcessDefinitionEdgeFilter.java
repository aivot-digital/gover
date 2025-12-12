package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionEdgeEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessDefinitionEdgeFilter implements Filter<ProcessDefinitionEdgeEntity> {
    private Integer id;
    private Integer processDefinitionId;
    private Integer processDefinitionVersion;
    private Integer fromNodeId;
    private Integer toNodeId;
    private String viaPort;

    public static ProcessDefinitionEdgeFilter create() {
        return new ProcessDefinitionEdgeFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessDefinitionEdgeEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessDefinitionEdgeEntity.class)
                .withEquals("id", id)
                .withEquals("processDefinitionId", processDefinitionId)
                .withEquals("processDefinitionVersion", processDefinitionVersion)
                .withEquals("fromNodeId", fromNodeId)
                .withEquals("toNodeId", toNodeId)
                .withContains("viaPort", viaPort);

        return builder.build();
    }

    public Integer getId() {
        return id;
    }

    public ProcessDefinitionEdgeFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionEdgeFilter setProcessDefinitionId(Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionEdgeFilter setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    public Integer getFromNodeId() {
        return fromNodeId;
    }

    public ProcessDefinitionEdgeFilter setFromNodeId(Integer fromNodeId) {
        this.fromNodeId = fromNodeId;
        return this;
    }

    public Integer getToNodeId() {
        return toNodeId;
    }

    public ProcessDefinitionEdgeFilter setToNodeId(Integer toNodeId) {
        this.toNodeId = toNodeId;
        return this;
    }

    public String getViaPort() {
        return viaPort;
    }

    public ProcessDefinitionEdgeFilter setViaPort(String viaPort) {
        this.viaPort = viaPort;
        return this;
    }
}

