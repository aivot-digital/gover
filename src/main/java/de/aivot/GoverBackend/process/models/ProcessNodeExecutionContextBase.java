package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class ProcessNodeExecutionContextBase {
    @Nonnull
    private final ProcessNodeExecutionLogger logger;
    @Nonnull
    private final ProcessNodeEntity thisNode;
    @Nonnull
    private final ProcessInstanceEntity thisProcessInstance;
    @Nonnull
    private final ProcessInstanceTaskEntity thisTask;
    @Nullable
    private final ProcessTestClaimEntity testClaim;

    public ProcessNodeExecutionContextBase(@Nonnull ProcessNodeExecutionLogger logger,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull ProcessInstanceEntity thisProcessInstance,
                                           @Nonnull ProcessInstanceTaskEntity thisTask,
                                           @Nullable ProcessTestClaimEntity testClaim) {
        this.logger = logger;
        this.thisNode = thisNode;
        this.thisProcessInstance = thisProcessInstance;
        this.thisTask = thisTask;
        this.testClaim = testClaim;
    }

    @Nonnull
    public ProcessNodeExecutionLogger getLogger() {
        return logger;
    }

    @Nonnull
    public ProcessNodeEntity getThisNode() {
        return thisNode;
    }

    @Nonnull
    public ProcessInstanceEntity getThisProcessInstance() {
        return thisProcessInstance;
    }

    @Nonnull
    public ProcessInstanceTaskEntity getThisTask() {
        return thisTask;
    }

    @Nullable
    public Object getTestClaim() {
        return testClaim;
    }
}
