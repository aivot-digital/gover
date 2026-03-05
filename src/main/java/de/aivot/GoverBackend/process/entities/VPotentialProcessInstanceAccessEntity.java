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
    private Boolean userIsDirectMember;

    @Nullable
    private Boolean userIsIndirectMember;

    @Nullable
    private List<String> userDirectPermissions;

    @Nullable
    private List<String> userIndirectPermissions;

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
                                                 @Nullable Boolean userIsDirectMember,
                                                 @Nullable Boolean userIsIndirectMember,
                                                 @Nullable List<String> userDirectPermissions,
                                                 @Nullable List<String> userIndirectPermissions,
                                                 @Nullable Integer viaProcessInstanceAccessPresetId,
                                                 @Nonnull List<String> permissions) {
        this.processId = processId;
        this.processVersion = processVersion;
        this.departmentId = departmentId;
        this.teamId = teamId;
        this.userId = userId;
        this.userViaDepartmentId = userViaDepartmentId;
        this.userViaTeamId = userViaTeamId;
        this.userIsDirectMember = userIsDirectMember;
        this.userIsIndirectMember = userIsIndirectMember;
        this.userDirectPermissions = userDirectPermissions;
        this.userIndirectPermissions = userIndirectPermissions;
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
    public Boolean getUserIsDirectMember() {
        return userIsDirectMember;
    }

    public VPotentialProcessInstanceAccessEntity setUserIsDirectMember(@Nullable Boolean userIsDirectMember) {
        this.userIsDirectMember = userIsDirectMember;
        return this;
    }

    @Nullable
    public Boolean getUserIsIndirectMember() {
        return userIsIndirectMember;
    }

    public VPotentialProcessInstanceAccessEntity setUserIsIndirectMember(@Nullable Boolean userIsIndirectMember) {
        this.userIsIndirectMember = userIsIndirectMember;
        return this;
    }

    @Nullable
    public List<String> getUserDirectPermissions() {
        return userDirectPermissions;
    }

    public VPotentialProcessInstanceAccessEntity setUserDirectPermissions(@Nullable List<String> userDirectPermissions) {
        this.userDirectPermissions = userDirectPermissions;
        return this;
    }

    @Nullable
    public List<String> getUserIndirectPermissions() {
        return userIndirectPermissions;
    }

    public VPotentialProcessInstanceAccessEntity setUserIndirectPermissions(@Nullable List<String> userIndirectPermissions) {
        this.userIndirectPermissions = userIndirectPermissions;
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
