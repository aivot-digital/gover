package de.aivot.prosuna.backend.process.models.executionResult;

import de.aivot.prosuna.backend.identity.models.IdentityData;
import jakarta.annotation.Nonnull;

public class ProcessNodeExecutionResultTaskAssignedCustomer extends ProcessNodeExecutionResult {
    @Nonnull
    private String identityId;

    // region constructor

    public ProcessNodeExecutionResultTaskAssignedCustomer() {
        this.identityId = "";
    }

    public ProcessNodeExecutionResultTaskAssignedCustomer(@Nonnull String identityId) {
        this.identityId = identityId;
    }

    // endregion

    // region factory methods

    public static ProcessNodeExecutionResultTaskAssignedCustomer of(@Nonnull String identityId) {
        return new ProcessNodeExecutionResultTaskAssignedCustomer(identityId);
    }

    public static ProcessNodeExecutionResultTaskAssignedCustomer of(@Nonnull IdentityData identityData) {
        return new ProcessNodeExecutionResultTaskAssignedCustomer(identityData.identityId());
    }

    // endregion


    @Nonnull
    public String getIdentityId() {
        return identityId;
    }

    public ProcessNodeExecutionResultTaskAssignedCustomer setIdentityId(@Nonnull String identityId) {
        this.identityId = identityId;
        return this;
    }
}
