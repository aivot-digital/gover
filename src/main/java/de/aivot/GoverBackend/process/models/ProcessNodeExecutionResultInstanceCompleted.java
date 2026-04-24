package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nullable;

import java.time.Instant;

public class ProcessNodeExecutionResultInstanceCompleted extends ProcessNodeExecutionResult {
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
