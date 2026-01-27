package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nullable;

import java.time.LocalDateTime;

public class ProcessNodeExecutionResultInstanceCompleted extends ProcessNodeExecutionResult {
    @Nullable
    private LocalDateTime retentionDate;

    @Nullable
    public LocalDateTime getRetentionDate() {
        return retentionDate;
    }

    public ProcessNodeExecutionResultInstanceCompleted setRetentionDate(@Nullable LocalDateTime retentionDate) {
        this.retentionDate = retentionDate;
        return this;
    }
}
