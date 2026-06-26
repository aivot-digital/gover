package de.aivot.gover.backend.process.filters;

import de.aivot.gover.backend.lib.models.EntityFilter;
import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import jakarta.annotation.Nonnull;

import java.util.UUID;

public class ProcessInstanceFilter extends EntityFilter<ProcessInstanceEntity> {
    private UUID accessKey;
    private Integer processId;
    private ProcessInstanceStatus status;
    private ProcessInstanceStatus statusIsNot;
    private String statusOverride;
    private String assignedFileNumber;
    private Integer createdForTestClaimId;

    public static ProcessInstanceFilter create() {
        return new ProcessInstanceFilter();
    }

    @Nonnull
    @Override
    public SpecificationBuilder<ProcessInstanceEntity> createSpecBuilder() {
        return SpecificationBuilder
                .create(ProcessInstanceEntity.class)
                .withEquals("accessKey", accessKey)
                .withEquals("processId", processId)
                .withEquals("status", status)
                .withNotEquals("status", statusIsNot)
                .withEquals("statusOverride", statusOverride)
                .withArrayContains("assignedFileNumbers", assignedFileNumber)
                .withEquals("createdForTestClaimId", createdForTestClaimId);
    }

    public UUID getAccessKey() {
        return accessKey;
    }

    public ProcessInstanceFilter setAccessKey(UUID accessKey) {
        this.accessKey = accessKey;
        return this;
    }

    public Integer getProcessId() {
        return processId;
    }

    public ProcessInstanceFilter setProcessId(Integer processId) {
        this.processId = processId;
        return this;
    }

    public ProcessInstanceStatus getStatus() {
        return status;
    }

    public ProcessInstanceFilter setStatus(ProcessInstanceStatus status) {
        this.status = status;
        return this;
    }

    public ProcessInstanceStatus getStatusIsNot() {
        return statusIsNot;
    }

    public ProcessInstanceFilter setStatusIsNot(ProcessInstanceStatus statusIsNot) {
        this.statusIsNot = statusIsNot;
        return this;
    }

    public String getStatusOverride() {
        return statusOverride;
    }

    public ProcessInstanceFilter setStatusOverride(String statusOverride) {
        this.statusOverride = statusOverride;
        return this;
    }

    public String getAssignedFileNumber() {
        return assignedFileNumber;
    }

    public ProcessInstanceFilter setAssignedFileNumber(String assignedFileNumber) {
        this.assignedFileNumber = assignedFileNumber;
        return this;
    }

    public Integer getCreatedForTestClaimId() {
        return createdForTestClaimId;
    }

    public ProcessInstanceFilter setCreatedForTestClaimId(Integer createdForTestClaimId) {
        this.createdForTestClaimId = createdForTestClaimId;
        return this;
    }
}