package de.aivot.prosuna.backend.process.models.processContext;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.entities.ProcessTestClaimEntity;
import de.aivot.prosuna.backend.process.models.ProcessNodeExecutionLogger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


public class ProcessNodeExecutionContextUICustomer extends ProcessNodeExecutionContextBase {
    @Nullable
    private final String identityId;

    public ProcessNodeExecutionContextUICustomer(@Nonnull ProcessNodeExecutionLogger logger,
                                                 @Nonnull ProcessNodeEntity thisNode,
                                                 @Nonnull ProcessInstanceEntity thisProcessInstance,
                                                 @Nonnull ProcessInstanceTaskEntity thisTask,
                                                 @Nullable ProcessTestClaimEntity testClaim,
                                                 @Nullable String identityId) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.identityId = identityId;
    }

    @Nullable
    public String getIdentityId() {
        return identityId;
    }
}
