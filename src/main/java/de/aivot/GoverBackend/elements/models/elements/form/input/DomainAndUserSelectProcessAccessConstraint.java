package de.aivot.GoverBackend.elements.models.elements.form.input;

import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public class DomainAndUserSelectProcessAccessConstraint {
    @Nullable
    private Integer processId;

    @Nullable
    private Integer processVersion;

    @Nullable
    private List<String> requiredPermissions;

    @Nullable
    public Integer getProcessId() {
        return processId;
    }

    public DomainAndUserSelectProcessAccessConstraint setProcessId(@Nullable Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nullable
    public Integer getProcessVersion() {
        return processVersion;
    }

    public DomainAndUserSelectProcessAccessConstraint setProcessVersion(@Nullable Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    @Nullable
    public List<String> getRequiredPermissions() {
        return requiredPermissions;
    }

    public DomainAndUserSelectProcessAccessConstraint setRequiredPermissions(@Nullable List<String> requiredPermissions) {
        this.requiredPermissions = requiredPermissions;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DomainAndUserSelectProcessAccessConstraint that = (DomainAndUserSelectProcessAccessConstraint) o;
        return Objects.equals(processId, that.processId) &&
               Objects.equals(processVersion, that.processVersion) &&
               Objects.equals(requiredPermissions, that.requiredPermissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, processVersion, requiredPermissions);
    }
}
