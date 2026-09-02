package de.aivot.prosuna.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class VUserProcessInstanceAccessPermissionsEntityId {
    @Nonnull
    private String userId;

    @Nullable
    private Integer viaSourceTeamId;

    @Nullable
    private Integer viaSourceDepartmentId;

    @Nonnull
    private Long targetProcessInstanceId;

    public VUserProcessInstanceAccessPermissionsEntityId() {
        userId = "";
        targetProcessInstanceId = 0L;
    }

    public VUserProcessInstanceAccessPermissionsEntityId(@Nonnull String userId,
                                                         @Nullable Integer viaSourceTeamId,
                                                         @Nullable Integer viaSourceDepartmentId,
                                                         @Nonnull Long targetProcessInstanceId) {
        this.userId = userId;
        this.viaSourceTeamId = viaSourceTeamId;
        this.viaSourceDepartmentId = viaSourceDepartmentId;
        this.targetProcessInstanceId = targetProcessInstanceId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VUserProcessInstanceAccessPermissionsEntityId that = (VUserProcessInstanceAccessPermissionsEntityId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(viaSourceTeamId, that.viaSourceTeamId) &&
                Objects.equals(viaSourceDepartmentId, that.viaSourceDepartmentId) && Objects.equals(targetProcessInstanceId, that.targetProcessInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, viaSourceTeamId, viaSourceDepartmentId, targetProcessInstanceId);
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VUserProcessInstanceAccessPermissionsEntityId setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public Integer getViaSourceTeamId() {
        return viaSourceTeamId;
    }

    public VUserProcessInstanceAccessPermissionsEntityId setViaSourceTeamId(@Nullable Integer viaSourceTeamId) {
        this.viaSourceTeamId = viaSourceTeamId;
        return this;
    }

    @Nullable
    public Integer getViaSourceDepartmentId() {
        return viaSourceDepartmentId;
    }

    public VUserProcessInstanceAccessPermissionsEntityId setViaSourceDepartmentId(@Nullable Integer viaSourceDepartmentId) {
        this.viaSourceDepartmentId = viaSourceDepartmentId;
        return this;
    }

    @Nonnull
    public Long getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public VUserProcessInstanceAccessPermissionsEntityId setTargetProcessInstanceId(@Nonnull Long targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
        return this;
    }
}
