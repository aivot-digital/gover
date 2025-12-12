package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

public class ProcessDefinitionVersionEntityId implements Serializable {
    @Nonnull
    private final Integer processDefinitionId;
    @Nonnull
    private final Integer processDefinitionVersion;

    // Default constructor
    public ProcessDefinitionVersionEntityId() {
        this.processDefinitionId = 0;
        this.processDefinitionVersion = 0;
    }

    // Full constructor
    public ProcessDefinitionVersionEntityId(@Nonnull Integer processDefinitionId, @Nonnull Integer processDefinitionVersion) {
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionVersion = processDefinitionVersion;
    }

    // Static factory method
    public static ProcessDefinitionVersionEntityId of(@Nonnull Integer processDefinitionId, @Nonnull Integer processDefinitionVersion) {
        return new ProcessDefinitionVersionEntityId(processDefinitionId, processDefinitionVersion);
    }

    // region Hash and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessDefinitionVersionEntityId that = (ProcessDefinitionVersionEntityId) o;
        return Objects.equals(processDefinitionId, that.processDefinitionId) && Objects.equals(processDefinitionVersion, that.processDefinitionVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processDefinitionId, processDefinitionVersion);
    }

    // endregion

    // region Getters

    @Nonnull
    public Integer getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Nonnull
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    // endregion
}