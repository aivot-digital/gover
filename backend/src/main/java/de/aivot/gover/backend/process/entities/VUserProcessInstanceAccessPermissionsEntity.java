package de.aivot.gover.backend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.List;
import java.util.Objects;

@Entity
@IdClass(VUserProcessInstanceAccessPermissionsEntityId.class)
@Table(name = "v_user_process_instance_access_permissions")
public class VUserProcessInstanceAccessPermissionsEntity {
    @Id
    @Nonnull
    private String userId;

    @Id
    @Nullable
    private Integer viaSourceTeamId;

    @Id
    @Nullable
    private Integer viaSourceDepartmentId;

    @Id
    @Nonnull
    private Long targetProcessInstanceId;

    @Nonnull
    private List<String> permissions;

    public VUserProcessInstanceAccessPermissionsEntity() {
    }

    public VUserProcessInstanceAccessPermissionsEntity(@Nonnull String userId,
                                                       @Nullable Integer viaSourceTeamId,
                                                       @Nullable Integer viaSourceDepartmentId,
                                                       @Nonnull Long targetProcessInstanceId,
                                                       @Nonnull List<String> permissions) {
        this.userId = userId;
        this.viaSourceTeamId = viaSourceTeamId;
        this.viaSourceDepartmentId = viaSourceDepartmentId;
        this.targetProcessInstanceId = targetProcessInstanceId;
        this.permissions = permissions;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VUserProcessInstanceAccessPermissionsEntity that = (VUserProcessInstanceAccessPermissionsEntity) o;
        return Objects.equals(userId, that.userId) && Objects.equals(viaSourceTeamId, that.viaSourceTeamId) &&
                Objects.equals(viaSourceDepartmentId, that.viaSourceDepartmentId) && Objects.equals(targetProcessInstanceId, that.targetProcessInstanceId) &&
                Objects.equals(permissions, that.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, viaSourceTeamId, viaSourceDepartmentId, targetProcessInstanceId, permissions);
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VUserProcessInstanceAccessPermissionsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public Integer getViaSourceTeamId() {
        return viaSourceTeamId;
    }

    public VUserProcessInstanceAccessPermissionsEntity setViaSourceTeamId(@Nullable Integer viaSourceTeamId) {
        this.viaSourceTeamId = viaSourceTeamId;
        return this;
    }

    @Nullable
    public Integer getViaSourceDepartmentId() {
        return viaSourceDepartmentId;
    }

    public VUserProcessInstanceAccessPermissionsEntity setViaSourceDepartmentId(@Nullable Integer viaSourceDepartmentId) {
        this.viaSourceDepartmentId = viaSourceDepartmentId;
        return this;
    }

    @Nonnull
    public Long getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public VUserProcessInstanceAccessPermissionsEntity setTargetProcessInstanceId(@Nonnull Long targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public VUserProcessInstanceAccessPermissionsEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
