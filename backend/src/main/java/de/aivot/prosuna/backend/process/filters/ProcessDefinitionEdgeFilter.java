package de.aivot.prosuna.backend.process.filters;

import de.aivot.prosuna.backend.process.entities.ProcessEdgeEntity;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.util.List;

public class ProcessDefinitionEdgeFilter implements Filter<ProcessEdgeEntity> {
    private Integer id;
    private Integer processDefinitionId;
    private List<Integer> processDefinitionIds;
    private Integer processDefinitionVersion;
    private Integer fromNodeId;
    private Integer toNodeId;
    private String viaPort;

    public static ProcessDefinitionEdgeFilter create() {
        return new ProcessDefinitionEdgeFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessEdgeEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessEdgeEntity.class)
                .withEquals("id", id)
                .withEquals("processId", processDefinitionId)
                .withInList("processId", processDefinitionIds)
                .withEquals("processVersion", processDefinitionVersion)
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

    public List<Integer> getProcessDefinitionIds() {
        return processDefinitionIds;
    }

    public ProcessDefinitionEdgeFilter setProcessDefinitionIds(List<Integer> processDefinitionIds) {
        this.processDefinitionIds = processDefinitionIds;
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
