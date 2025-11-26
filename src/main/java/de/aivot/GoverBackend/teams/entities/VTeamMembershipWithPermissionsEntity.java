package de.aivot.GoverBackend.teams.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_team_memberships_with_permissions")
public class VTeamMembershipWithPermissionsEntity {
    @Id
    private Integer id;
    private Integer teamId;
    private String userId;
    private LocalDateTime created;
    private LocalDateTime updated;
    private Boolean teamPermissionEdit;
    private Boolean formPermissionCreate;
    private Boolean formPermissionRead;
    private Boolean formPermissionEdit;
    private Boolean formPermissionDelete;
    private Boolean formPermissionAnnotate;
    private Boolean formPermissionPublish;
    private Boolean processPermissionCreate;
    private Boolean processPermissionRead;
    private Boolean processPermissionEdit;
    private Boolean processPermissionDelete;
    private Boolean processPermissionAnnotate;
    private Boolean processPermissionPublish;
    private Boolean processInstancePermissionCreate;
    private Boolean processInstancePermissionRead;
    private Boolean processInstancePermissionEdit;
    private Boolean processInstancePermissionDelete;
    private Boolean processInstancePermissionAnnotate;

    public Integer getId() {
        return id;
    }

    public VTeamMembershipWithPermissionsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithPermissionsEntity setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithPermissionsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VTeamMembershipWithPermissionsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VTeamMembershipWithPermissionsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public Boolean getTeamPermissionEdit() {
        return teamPermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setTeamPermissionEdit(Boolean teamPermissionEdit) {
        this.teamPermissionEdit = teamPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VTeamMembershipWithPermissionsEntity setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionCreate(Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionRead(Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionEdit(Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionDelete(Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionAnnotate(Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public VTeamMembershipWithPermissionsEntity setProcessPermissionPublish(Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionCreate(Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionRead(Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionEdit(Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionDelete(Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public VTeamMembershipWithPermissionsEntity setProcessInstancePermissionAnnotate(Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }
}

