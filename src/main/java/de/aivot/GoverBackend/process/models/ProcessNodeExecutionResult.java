package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nullable;

import java.util.Map;

public abstract class ProcessNodeExecutionResult {
    private @Nullable Map<String, Object> runtimeData;
    private @Nullable Map<String, Object> nodeData;
    private @Nullable Map<String, Object> processData;

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
}
