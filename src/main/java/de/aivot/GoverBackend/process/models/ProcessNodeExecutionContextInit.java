package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionContextInit extends ProcessNodeExecutionContextBase {
    /**
     * The process data available during the execution of the node.
     */
    @Nonnull
    private final ProcessExecutionData processExecutionData;

    /**
     * The configuration data available during the execution of the node.
     */
    private final DerivedRuntimeElementData configuration;

    public ProcessNodeExecutionContextInit(@Nonnull ProcessNodeExecutionLogger logger,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull ProcessInstanceEntity thisProcessInstance,
                                           @Nonnull ProcessInstanceTaskEntity thisTask,
                                           @Nullable ProcessTestClaimEntity testClaim,
                                           @Nonnull ProcessExecutionData processExecutionData,
                                           @Nonnull DerivedRuntimeElementData configuration) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.processExecutionData = processExecutionData;
        this.configuration = configuration;
    }

    @Nonnull
    public ProcessExecutionData getProcessExecutionData() {
        return processExecutionData;
    }

    public DerivedRuntimeElementData getConfiguration() {
        return configuration;
    }
}
