package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;

public class ProcessNodeExecutionResultTaskAssigned extends ProcessNodeExecutionResult {
    @Nonnull
    private String assignedUserId;

    // region constructor

    public ProcessNodeExecutionResultTaskAssigned() {
        this.assignedUserId = "";
    }

    public ProcessNodeExecutionResultTaskAssigned(@Nonnull String userId) {
        this.assignedUserId = userId;
    }

    // endregion

    // region factory methods

    public static ProcessNodeExecutionResultTaskAssigned of(@Nonnull String userId) {
        return new ProcessNodeExecutionResultTaskAssigned(userId);
    }

    public static ProcessNodeExecutionResultTaskAssigned of(@Nonnull UserEntity user) {
        return new ProcessNodeExecutionResultTaskAssigned(user.getId());
    }

    // endregion

    @Nonnull
    public String getAssignedUserId() {
        return assignedUserId;
    }

    public ProcessNodeExecutionResultTaskAssigned setAssignedUserId(@Nonnull String assignedUserId) {
        this.assignedUserId = assignedUserId;
        return this;
    }
}
