package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
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
