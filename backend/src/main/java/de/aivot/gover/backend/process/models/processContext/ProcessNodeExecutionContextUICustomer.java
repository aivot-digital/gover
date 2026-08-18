package de.aivot.gover.backend.process.models.processContext;

import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.gover.backend.process.entities.ProcessNodeEntity;
import de.aivot.gover.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.gover.backend.process.models.ProcessNodeExecutionLogger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;


public class ProcessNodeExecutionContextUICustomer<NodeConfig> extends ProcessNodeExecutionContextBase {
    @Nullable
    private final String identityId;

    @Nonnull
    private final NodeConfig configurationOfExecutingNode;

    @Nullable
    private final Map<String, List<String>> queryParameters;

    public ProcessNodeExecutionContextUICustomer(@Nonnull ProcessNodeExecutionLogger logger,
                                                 @Nonnull ProcessNodeEntity thisNode,
                                                 @Nonnull ProcessInstanceEntity thisProcessInstance,
                                                 @Nonnull ProcessInstanceTaskEntity thisTask,
                                                 @Nullable ProcessTestClaimEntity testClaim,
                                                 @Nullable String identityId,
                                                 @Nonnull NodeConfig configurationOfExecutingNode,
                                                 @Nullable Map<String, List<String>> queryParameters) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.identityId = identityId;
        this.configurationOfExecutingNode = configurationOfExecutingNode;
        this.queryParameters = queryParameters;
    }

    @Nullable
    public String getIdentityId() {
        return identityId;
    }

    @Nonnull
    public NodeConfig getConfigurationOfExecutingNode() {
        return configurationOfExecutingNode;
    }

    @Nullable
    public Map<String, List<String>> getQueryParameters() {
        return queryParameters;
    }
}
