package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.domain.Specification;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ProcessNodeFilter implements Filter<ProcessNodeEntity> {
    private Integer id;
    private Integer notId;
    private String name;
    private Integer processId;
    private Integer processVersion;
    private String dataKey;
    private String processNodeDefinitionKey;
    private String processNodeDefinitionVersion;

    private Map<String, String> configEquals = new HashMap<>();

    private List<Specification<ProcessNodeEntity>> additionalSpecifications = new LinkedList<>();

    public static ProcessNodeFilter create() {
        return new ProcessNodeFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessNodeEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessNodeEntity.class)
                .withEquals("id", id)
                .withNotEquals("id", notId)
                .withEquals("processId", processId)
                .withEquals("processVersion", processVersion)
                .withContains("name", name)
                .withEquals("dataKey", dataKey)
                .withEquals("processNodeDefinitionKey", processNodeDefinitionKey)
                .withEquals("processNodeDefinitionVersion", processNodeDefinitionVersion);

        for (var entry : configEquals.entrySet()) {
            builder = builder
                    .withJsonEquals("configuration", List.of(entry.getKey().split("\\.")), entry.getValue());
        }

        for (var spec : additionalSpecifications) {
            builder = builder.withSpecification(spec);
        }

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

    public ProcessNodeFilter addConfigEquals(String field, String value) {
        configEquals.put(field, value);
        return this;
    }

    public ProcessNodeFilter addAdditionalSpecification(Specification<ProcessNodeEntity> specification) {
        additionalSpecifications.add(specification);
        return this;
    }

    public Integer getNotId() {
        return notId;
    }

    public ProcessNodeFilter setNotId(Integer notId) {
        this.notId = notId;
        return this;
    }
}

