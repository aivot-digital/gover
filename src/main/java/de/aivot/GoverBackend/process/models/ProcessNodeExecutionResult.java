package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class ProcessNodeExecutionResult {
    private @Nullable Map<String, Object> metadata;
    private @Nullable Map<String, Object> output;

    @Nullable
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public ProcessNodeExecutionResult setMetadata(@Nullable Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    @Nullable
    public Map<String, Object> getOutput() {
        return output;
    }

    public ProcessNodeExecutionResult setOutput(@Nullable Map<String, Object> output) {
        this.output = output;
        return this;
    }
}
