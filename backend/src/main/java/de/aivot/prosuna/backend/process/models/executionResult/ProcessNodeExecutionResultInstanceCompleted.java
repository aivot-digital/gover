package de.aivot.prosuna.backend.process.models.executionResult;

import jakarta.annotation.Nullable;

import java.time.Instant;

public class ProcessNodeExecutionResultInstanceCompleted extends ProcessNodeExecutionResult {
    /** Optional absolute override; without it, the finishing node's process version provides the retention period. */
    @Nullable
    private Instant retentionDate;

    @Nullable
    public Instant getRetentionDate() {
        return retentionDate;
    }

    public ProcessNodeExecutionResultInstanceCompleted setRetentionDate(@Nullable Instant retentionDate) {
        this.retentionDate = retentionDate;
        return this;
    }
}
