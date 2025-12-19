package de.aivot.GoverBackend.process.models;

import jakarta.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public abstract class ProcessNodeExecutionResult {
    private @Nullable Map<String, Object> runtimeData;
    private @Nullable Map<String, Object> nodeData;
    private @Nullable Map<String, Object> processData;
    private @Nullable List<ProcessNodeExecutionEvent> events;
    private @Nullable String taskStatusOverride;
    private @Nullable Boolean clearTaskStatusOverride;


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
    public List<ProcessNodeExecutionEvent> getEvents() {
        return events;
    }

    public ProcessNodeExecutionResult setEvents(@Nullable List<ProcessNodeExecutionEvent> events) {
        this.events = events;
        return this;
    }

    public ProcessNodeExecutionResult addEvent(ProcessNodeExecutionEvent event) {
        if (this.events == null) {
            this.events = new LinkedList<>();
        }
        this.events.add(event);
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
}
