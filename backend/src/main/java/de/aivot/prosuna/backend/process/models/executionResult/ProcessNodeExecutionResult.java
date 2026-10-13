package de.aivot.prosuna.backend.process.models.executionResult;

import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

public abstract class ProcessNodeExecutionResult {
    private @Nullable Map<String, Object> runtimeData;
    private @Nullable Map<String, Object> nodeData;
    private @Nullable Map<String, Object> processData;
    private @Nullable String taskStatusOverride;
    private @Nullable Boolean clearTaskStatusOverride;
    private @Nullable ProcessNodeExecutionResultCommunicationRequest communicationRequest;

    /**
     * Clear the current task's staff assignment when true, including for no-op results.
     * Null and false preserve the assignment. Cannot be combined with a new staff task assignment.
     * Clearing the assignment does not change the instance assignment or participation history.
     */
    private @Nullable Boolean clearCurrentlyAssignedUser;

    @Nullable
    public Map<String, Object> getRuntimeData() {
        return runtimeData;
    }

    public ProcessNodeExecutionResult setRuntimeData(@Nullable Map<String, Object> runtimeData) {
        this.runtimeData = runtimeData;
        return this;
    }

    @Nullable
    public Map<String, Object> getNodeData() {
        return nodeData;
    }

    public ProcessNodeExecutionResult setNodeData(@Nullable Map<String, Object> nodeData) {
        this.nodeData = nodeData;
        return this;
    }

    @Nullable
    public Map<String, Object> getProcessData() {
        return processData;
    }

    public ProcessNodeExecutionResult setProcessData(@Nullable Map<String, Object> processData) {
        this.processData = processData;
        return this;
    }

    @Nullable
    public String getTaskStatusOverride() {
        return taskStatusOverride;
    }

    public ProcessNodeExecutionResult setTaskStatusOverride(@Nullable String taskStatusOverride) {
        this.taskStatusOverride = taskStatusOverride;
        return this;
    }

    @Nullable
    public Boolean getClearTaskStatusOverride() {
        return clearTaskStatusOverride;
    }

    public ProcessNodeExecutionResult setClearTaskStatusOverride(@Nullable Boolean clearTaskStatusOverride) {
        this.clearTaskStatusOverride = clearTaskStatusOverride;
        return this;
    }

    @Nullable
    public ProcessNodeExecutionResultCommunicationRequest getCommunicationRequest() {
        return communicationRequest;
    }

    public ProcessNodeExecutionResult setCommunicationRequest(
            @Nullable ProcessNodeExecutionResultCommunicationRequest communicationRequest
    ) {
        this.communicationRequest = communicationRequest;
        return this;
    }

    public Optional<ProcessNodeExecutionResult> asOptional() {
        return Optional.of(this);
    }

    @Nullable
    public Boolean getClearCurrentlyAssignedUser() {
        return clearCurrentlyAssignedUser;
    }

    public ProcessNodeExecutionResult setClearCurrentlyAssignedUser(@Nullable Boolean clearCurrentlyAssignedUser) {
        this.clearCurrentlyAssignedUser = clearCurrentlyAssignedUser;
        return this;
    }
}
