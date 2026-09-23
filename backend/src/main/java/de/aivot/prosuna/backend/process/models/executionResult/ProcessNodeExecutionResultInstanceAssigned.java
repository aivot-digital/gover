package de.aivot.prosuna.backend.process.models.executionResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionResultInstanceAssigned extends ProcessNodeExecutionResult {
    @Nullable
    private String assignedUserId;

    // region constructor

    private ProcessNodeExecutionResultInstanceAssigned(@Nullable String assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    // endregion

    // region factory methods

    public static ProcessNodeExecutionResultInstanceAssigned assign(@Nonnull String userId) {
        return new ProcessNodeExecutionResultInstanceAssigned(userId);
    }

    public static ProcessNodeExecutionResultInstanceAssigned clear() {
        return new ProcessNodeExecutionResultInstanceAssigned(null);
    }

    // endregion

    // region getters and setters

    @Nullable
    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessNodeExecutionResultInstanceAssigned setAssignedUserId(@Nullable String assignedUserId) {
        this.assignedUserId = assignedUserId;
        return this;
    }

    // endregion
}
