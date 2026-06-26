package de.aivot.gover.backend.process.models.processContext;

import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.gover.backend.process.models.ProcessExecutionData;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import de.aivot.gover.backend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


public class ProcessNodeExecutionContextUIStaff<NodeConfig> extends ProcessNodeExecutionContextBase {
    @Nonnull
    private final UserEntity callingUser;

    @Nonnull
    private final NodeConfig configurationOfExecutingNode;

    @Nonnull
    private final ProcessExecutionData currentProcessExecutionData;

    public ProcessNodeExecutionContextUIStaff(@Nonnull ProcessNodeExecutionLogger logger,
                                              @Nonnull ProcessNodeEntity thisNode,
                                              @Nonnull ProcessInstanceEntity thisProcessInstance,
                                              @Nonnull ProcessInstanceTaskEntity thisTask,
                                              @Nullable ProcessTestClaimEntity testClaim,
                                              @Nonnull UserEntity callingUser,
                                              @Nonnull NodeConfig configurationOfExecutingNode,
                                              @Nonnull ProcessExecutionData currentProcessExecutionData) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.callingUser = callingUser;
        this.configurationOfExecutingNode = configurationOfExecutingNode;
        this.currentProcessExecutionData = currentProcessExecutionData;
    }

    @Nonnull
    public UserEntity getCallingUser() {
        return callingUser;
    }

    @Nonnull
    public NodeConfig getConfigurationOfExecutingNode() {
        return configurationOfExecutingNode;
    }

    @Nonnull
    public ProcessExecutionData getCurrentProcessExecutionData() {
        return currentProcessExecutionData;
    }
}
