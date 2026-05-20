package de.aivot.GoverBackend.process.models.processContext;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.process.models.ProcessExecutionData;
import de.aivot.GoverBackend.process.models.ProcessNodeExecutionLogger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ProcessNodeExecutionInitContext<NodeConfig> extends ProcessNodeExecutionContextBase {
    /**
     * The process data available during the execution of the node.
     */
    @Nonnull
    private final ProcessExecutionData currentProcessExecutionData;

    /**
     * The configuration data available during the execution of the node.
     */
    @Nonnull
    private final NodeConfig configurationOfExecutingNode;

    public ProcessNodeExecutionInitContext(@Nonnull ProcessNodeExecutionLogger logger,
                                           @Nonnull ProcessNodeEntity thisNode,
                                           @Nonnull ProcessInstanceEntity thisProcessInstance,
                                           @Nonnull ProcessInstanceTaskEntity thisTask,
                                           @Nullable ProcessTestClaimEntity testClaim,
                                           @Nonnull ProcessExecutionData currentProcessExecutionData,
                                           @Nonnull NodeConfig configurationOfExecutingNode) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.currentProcessExecutionData = currentProcessExecutionData;
        this.configurationOfExecutingNode = configurationOfExecutingNode;
    }

    @Nonnull
    public ProcessExecutionData getCurrentProcessExecutionData() {
        return currentProcessExecutionData;
    }

    @Nonnull
    public NodeConfig getConfigurationOfExecutingNode() {
        return configurationOfExecutingNode;
    }
}
