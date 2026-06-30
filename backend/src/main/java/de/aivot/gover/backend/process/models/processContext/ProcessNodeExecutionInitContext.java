package de.aivot.gover.backend.process.models.processContext;

import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
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
