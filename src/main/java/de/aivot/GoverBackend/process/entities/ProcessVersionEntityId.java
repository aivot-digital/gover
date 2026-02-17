package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

public class ProcessVersionEntityId implements Serializable {
    @Nonnull
    private final Integer processId;
    @Nonnull
    private final Integer processVersion;

    // Default constructor
    public ProcessVersionEntityId() {
        this.processId = 0;
        this.processVersion = 0;
    }

    // Full constructor
    public ProcessVersionEntityId(@Nonnull Integer processId, @Nonnull Integer processVersion) {
        this.processId = processId;
        this.processVersion = processVersion;
    }

    // Static factory method
    public static ProcessVersionEntityId of(@Nonnull Integer processDefinitionId, @Nonnull Integer processDefinitionVersion) {
        return new ProcessVersionEntityId(processDefinitionId, processDefinitionVersion);
    }

    // region Hash and Equals

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ProcessVersionEntityId that = (ProcessVersionEntityId) o;
        return Objects.equals(processId, that.processId) && Objects.equals(processVersion, that.processVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, processVersion);
    }

    // endregion

    // region Getters

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    // endregion
}