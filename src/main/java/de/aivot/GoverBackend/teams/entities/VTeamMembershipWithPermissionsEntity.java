package de.aivot.GoverBackend.teams.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;

@Entity
@Table(name = "v_team_memberships_with_permissions")
public class VTeamMembershipWithPermissionsEntity {
    @Id
    @Nonnull
    private Integer membershipId;

    @Nonnull
    private Integer teamId;

    @Nonnull
    private String userId;

    @Nonnull
    private Boolean teamMemberPermissionEdit;

    @Nonnull
    private Boolean formPermissionCreate;

    @Nonnull
    private Boolean formPermissionRead;

    @Nonnull
    private Boolean formPermissionEdit;

    @Nonnull
    private Boolean formPermissionDelete;

    @Nonnull
    private Boolean formPermissionAnnotate;

    @Nonnull
    private Boolean formPermissionPublish;

    @Nonnull
    private Boolean processPermissionCreate;

    @Nonnull
    private Boolean processPermissionRead;

    @Nonnull
    private Boolean processPermissionEdit;

    @Nonnull
    private Boolean processPermissionDelete;

    @Nonnull
    private Boolean processPermissionAnnotate;

    @Nonnull
    private Boolean processPermissionPublish;

    @Nonnull
    private Boolean processInstancePermissionCreate;

    @Nonnull
    private Boolean processInstancePermissionRead;

    @Nonnull
    private Boolean processInstancePermissionEdit;

    @Nonnull
    private Boolean processInstancePermissionDelete;

    @Nonnull
    private Boolean processInstancePermissionAnnotate;

    // region Getters and Setters

    @Nonnull
    public Integer getMembershipId() {
        return membershipId;
    }

    public VTeamMembershipWithPermissionsEntity setMembershipId(@Nonnull Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    @Nonnull
    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithPermissionsEntity setTeamId(@Nonnull Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithPermissionsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public Boolean getTeamMemberPermissionEdit() {
        return teamMemberPermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setTeamMemberPermissionEdit(@Nonnull Boolean permissionEditTeam) {
        this.teamMemberPermissionEdit = permissionEditTeam;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionCreate(@Nonnull Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionRead(@Nonnull Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionEdit(@Nonnull Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionDelete(@Nonnull Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionAnnotate(@Nonnull Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionPublish(@Nonnull Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionCreate(@Nonnull Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionRead(@Nonnull Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionEdit(@Nonnull Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionDelete(@Nonnull Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionAnnotate(@Nonnull Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionPublish(@Nonnull Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionCreate(@Nonnull Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionRead(@Nonnull Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionEdit(@Nonnull Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionDelete(@Nonnull Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionAnnotate(@Nonnull Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }

    // endregion
}

