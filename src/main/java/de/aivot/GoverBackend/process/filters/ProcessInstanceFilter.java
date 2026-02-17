package de.aivot.GoverBackend.process.filters;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.process.enums.ProcessInstanceStatus;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessInstanceFilter implements Filter<ProcessInstanceEntity> {
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
    public Specification<ProcessInstanceEntity> build() {
        var builder = SpecificationBuilder
                .create(ProcessInstanceEntity.class)
                .withEquals("accessKey", accessKey)
                .withEquals("processId", processId)
                .withEquals("status", status)
                .withNotEquals("status", statusIsNot)
                .withEquals("statusOverride", statusOverride)
                .withArrayContains("assignedFileNumbers", assignedFileNumber)
                .withEquals("createdForTestClaimId", createdForTestClaimId);

        return builder.build();
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

