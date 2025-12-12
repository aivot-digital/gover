package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionVersionEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;

public class ProcessDefinitionVersionFilter implements Filter<ProcessDefinitionVersionEntity> {
    private Integer processDefinitionId;
    private Short processDefinitionVersion;
    private Short status;
    private Short retentionTimeUnit;
    private Integer retentionTimeAmount;

    public static ProcessDefinitionVersionFilter create() {
        return new ProcessDefinitionVersionFilter();
    }

    @Nonnull
    @Override
    public Specification<ProcessDefinitionVersionEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessDefinitionVersionEntity.class)
                .withEquals("processDefinitionId", processDefinitionId)
                .withEquals("processDefinitionVersion", processDefinitionVersion)
                .withEquals("status", status)
                .withEquals("retentionTimeUnit", retentionTimeUnit)
                .withEquals("retentionTimeAmount", retentionTimeAmount);

        return builder.build();
    }

    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    public ProcessDefinitionVersionFilter setProcessDefinitionId(Integer processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    public Short getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    public ProcessDefinitionVersionFilter setProcessDefinitionVersion(Short processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
        return this;
    }

    public Short getStatus() {
        return status;
    }

    public ProcessDefinitionVersionFilter setStatus(Short status) {
        this.status = status;
        return this;
    }

    public Short getRetentionTimeUnit() {
        return retentionTimeUnit;
    }

    public ProcessDefinitionVersionFilter setRetentionTimeUnit(Short retentionTimeUnit) {
        this.retentionTimeUnit = retentionTimeUnit;
        return this;
    }

    public Integer getRetentionTimeAmount() {
        return retentionTimeAmount;
    }

    public ProcessDefinitionVersionFilter setRetentionTimeAmount(Integer retentionTimeAmount) {
        this.retentionTimeAmount = retentionTimeAmount;
        return this;
    }
}

