package de.aivot.GoverBackend.process.models;

import de.aivot.GoverBackend.elements.models.DerivedRuntimeElementData;
import de.aivot.GoverBackend.process.entities.ProcessInstanceEntity;
import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import de.aivot.GoverBackend.user.entities.UserEntity;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;


public class ProcessNodeExecutionContextUIStaff extends ProcessNodeExecutionContextBase {
    @Nonnull
    private final UserEntity user;

    @Nonnull
    private final DerivedRuntimeElementData runtimeElementData;

    @Nonnull
    private final ProcessExecutionData processData;

    public ProcessNodeExecutionContextUIStaff(@Nonnull ProcessNodeExecutionLogger logger,
                                              @Nonnull ProcessNodeEntity thisNode,
                                              @Nonnull ProcessInstanceEntity thisProcessInstance,
                                              @Nonnull ProcessInstanceTaskEntity thisTask,
                                              @Nullable ProcessTestClaimEntity testClaim,
                                              @Nonnull UserEntity user,
                                              @Nonnull DerivedRuntimeElementData runtimeElementData,
                                              @Nonnull ProcessExecutionData processData) {
        super(logger, thisNode, thisProcessInstance, thisTask, testClaim);
        this.user = user;
        this.runtimeElementData = runtimeElementData;
        this.processData = processData;
    }

    @Nonnull
    public UserEntity getUser() {
        return user;
    }

    @Nonnull
    public DerivedRuntimeElementData getRuntimeElementData() {
        return runtimeElementData;
    }

    @Nonnull
    public ProcessExecutionData getProcessData() {
        return processData;
    }
}
