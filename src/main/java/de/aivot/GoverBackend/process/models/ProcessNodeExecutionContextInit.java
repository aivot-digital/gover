package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

public class ProcessNodeExecutionContextInit extends ProcessNodeExecutionContextBase {
    /**
     * The process data available during the execution of the node.
     */
    @Nonnull
    private final Map<String, Object> processData;

    public ProcessNodeExecutionContextInit(@Nonnull ProcessNodeExecutionLogger logger,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull ProcessInstanceEntity thisProcessInstance,
                                           @Nonnull ProcessInstanceTaskEntity thisTask,
                                           @Nullable ProcessTestClaimEntity testClaim,
                                           @Nonnull ProcessExecutionData processData) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.processData = processData;
    }

    @Nonnull
    public Map<String, Object> getProcessData() {
        return processData;
    }
}
