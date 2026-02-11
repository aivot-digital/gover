package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class VPotentialProcessInstanceAccessEntityId {
    @Nonnull
    private Integer processId;

    @Nonnull
    private Integer processVersion;

    @Nullable
    private Integer departmentId;

    @Nullable
    private Integer teamId;

    @Nullable
    private String userId;

    public VPotentialProcessInstanceAccessEntityId() {
        processId = 0;
        processVersion = 0;
    }

    public VPotentialProcessInstanceAccessEntityId(@Nonnull Integer processId,
                                                   @Nonnull Integer processVersion,
                                                   @Nullable Integer departmentId,
                                                   @Nullable Integer teamId,
                                                   @Nullable String userId) {
        this.processId = processId;
        this.processVersion = processVersion;
        this.departmentId = departmentId;
        this.teamId = teamId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VPotentialProcessInstanceAccessEntityId that = (VPotentialProcessInstanceAccessEntityId) o;
        return Objects.equals(processId, that.processId) && Objects.equals(processVersion, that.processVersion) && Objects.equals(departmentId, that.departmentId) && Objects.equals(teamId, that.teamId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processId, processVersion, departmentId, teamId, userId);
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public VPotentialProcessInstanceAccessEntityId setProcessId(@Nonnull Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public VPotentialProcessInstanceAccessEntityId setProcessVersion(@Nonnull Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    @Nullable
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VPotentialProcessInstanceAccessEntityId setDepartmentId(@Nullable Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nullable
    public Integer getTeamId() {
        return teamId;
    }

    public VPotentialProcessInstanceAccessEntityId setTeamId(@Nullable Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    public VPotentialProcessInstanceAccessEntityId setUserId(@Nullable String userId) {
        this.userId = userId;
        return this;
    }
}
