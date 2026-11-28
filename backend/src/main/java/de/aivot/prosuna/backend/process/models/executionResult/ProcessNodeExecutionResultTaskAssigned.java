package de.aivot.prosuna.backend.process.models.executionResult;

import de.aivot.prosuna.backend.process.models.ProcessTaskAssignmentPool;
import de.aivot.prosuna.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionResultTaskAssigned extends ProcessNodeExecutionResult {
    @Nonnull
    private String assignedUserId;

    // Stored on the task so manual reassignments stay within the element's Personenkreis.
    @Nullable
    private ProcessTaskAssignmentPool assignmentPool;

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

    @Nullable
    public ProcessTaskAssignmentPool getAssignmentPool() {
        return assignmentPool;
    }

    public ProcessNodeExecutionResultTaskAssigned setAssignmentPool(@Nullable ProcessTaskAssignmentPool assignmentPool) {
        this.assignmentPool = assignmentPool;
        return this;
    }
}
