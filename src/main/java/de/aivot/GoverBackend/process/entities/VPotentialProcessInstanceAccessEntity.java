package de.aivot.GoverBackend.process.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@IdClass(VPotentialProcessInstanceAccessEntityId.class)
@Table(name = "v_potential_process_instance_access")
public class VPotentialProcessInstanceAccessEntity {
    @Id
    @Nonnull
    private Integer processId;

    @Id
    @Nonnull
    private Integer processVersion;

    @Id
    @Nullable
    private Integer departmentId;

    @Id
    @Nullable
    private Integer teamId;

    @Id
    @Nullable
    private String userId;

    @Nullable
    private Integer userViaDepartmentId;

    @Nullable
    private Integer userViaTeamId;

    @Nullable
    private Integer viaProcessInstanceAccessPresetId;

    @Nonnull
    private List<String> permissions;

    public VPotentialProcessInstanceAccessEntity() {
    }

    public VPotentialProcessInstanceAccessEntity(@Nonnull Integer processId,
                                                 @Nonnull Integer processVersion,
                                                 @Nullable Integer departmentId,
                                                 @Nullable Integer teamId,
                                                 @Nullable String userId,
                                                 @Nullable Integer userViaDepartmentId,
                                                 @Nullable Integer userViaTeamId,
                                                 @Nullable Integer viaProcessInstanceAccessPresetId,
                                                 @Nonnull List<String> permissions) {
        this.processId = processId;
        this.processVersion = processVersion;
        this.departmentId = departmentId;
        this.teamId = teamId;
        this.userId = userId;
        this.userViaDepartmentId = userViaDepartmentId;
        this.userViaTeamId = userViaTeamId;
        this.viaProcessInstanceAccessPresetId = viaProcessInstanceAccessPresetId;
        this.permissions = permissions;
    }

    @Nonnull
    public Integer getProcessId() {
        return processId;
    }

    public VPotentialProcessInstanceAccessEntity setProcessId(@Nonnull Integer processId) {
        this.processId = processId;
        return this;
    }

    @Nonnull
    public Integer getProcessVersion() {
        return processVersion;
    }

    public VPotentialProcessInstanceAccessEntity setProcessVersion(@Nonnull Integer processVersion) {
        this.processVersion = processVersion;
        return this;
    }

    @Nullable
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VPotentialProcessInstanceAccessEntity setDepartmentId(@Nullable Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nullable
    public Integer getTeamId() {
        return teamId;
    }

    public VPotentialProcessInstanceAccessEntity setTeamId(@Nullable Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    @Nullable
    public String getUserId() {
        return userId;
    }

    public VPotentialProcessInstanceAccessEntity setUserId(@Nullable String userId) {
        this.userId = userId;
        return this;
    }

    @Nullable
    public Integer getUserViaDepartmentId() {
        return userViaDepartmentId;
    }

    public VPotentialProcessInstanceAccessEntity setUserViaDepartmentId(@Nullable Integer userViaDepartmentId) {
        this.userViaDepartmentId = userViaDepartmentId;
        return this;
    }

    @Nullable
    public Integer getUserViaTeamId() {
        return userViaTeamId;
    }

    public VPotentialProcessInstanceAccessEntity setUserViaTeamId(@Nullable Integer userViaTeamId) {
        this.userViaTeamId = userViaTeamId;
        return this;
    }

    @Nullable
    public Integer getViaProcessInstanceAccessPresetId() {
        return viaProcessInstanceAccessPresetId;
    }

    public VPotentialProcessInstanceAccessEntity setViaProcessInstanceAccessPresetId(@Nullable Integer viaProcessInstanceAccessPresetId) {
        this.viaProcessInstanceAccessPresetId = viaProcessInstanceAccessPresetId;
        return this;
    }

    @Nonnull
    public List<String> getPermissions() {
        return permissions;
    }

    public VPotentialProcessInstanceAccessEntity setPermissions(@Nonnull List<String> permissions) {
        this.permissions = permissions;
        return this;
    }
}
