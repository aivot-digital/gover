package de.aivot.gover.backend.process.models.processContext;

import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
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
