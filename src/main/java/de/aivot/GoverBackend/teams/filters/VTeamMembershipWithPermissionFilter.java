package de.aivot.GoverBackend.teams.filters;

import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.teams.entities.VTeamMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

public class VTeamMembershipWithPermissionFilter implements Filter<VTeamMembershipWithPermissionsEntity> {
    private Integer teamId;
    private String userId;
    private Boolean permissionEditTeam;
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

    public static VTeamMembershipWithPermissionFilter create() {
        return new VTeamMembershipWithPermissionFilter();
    }

    @Nonnull
    @Override
    public Specification<VTeamMembershipWithPermissionsEntity> build() {
        return SpecificationBuilder
                .create(VTeamMembershipWithPermissionsEntity.class)
                .withEquals("teamId", teamId)
                .withEquals("userId", userId)
                .withEquals("permissionEditTeam", permissionEditTeam)
                .withEquals("formPermissionCreate", formPermissionCreate)
                .withEquals("formPermissionRead", formPermissionRead)
                .withEquals("formPermissionEdit", formPermissionEdit)
                .withEquals("formPermissionDelete", formPermissionDelete)
                .withEquals("formPermissionAnnotate", formPermissionAnnotate)
                .withEquals("formPermissionPublish", formPermissionPublish)
                .withEquals("processPermissionCreate", processPermissionCreate)
                .withEquals("processPermissionRead", processPermissionRead)
                .withEquals("processPermissionEdit", processPermissionEdit)
                .withEquals("processPermissionDelete", processPermissionDelete)
                .withEquals("processPermissionAnnotate", processPermissionAnnotate)
                .withEquals("processPermissionPublish", processPermissionPublish)
                .withEquals("processInstancePermissionCreate", processInstancePermissionCreate)
                .withEquals("processInstancePermissionRead", processInstancePermissionRead)
                .withEquals("processInstancePermissionEdit", processInstancePermissionEdit)
                .withEquals("processInstancePermissionDelete", processInstancePermissionDelete)
                .withEquals("processInstancePermissionAnnotate", processInstancePermissionAnnotate)
                .build();
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamMembershipWithPermissionFilter setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VTeamMembershipWithPermissionFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getPermissionEditTeam() {
        return permissionEditTeam;
    }

    public VTeamMembershipWithPermissionFilter setPermissionEditTeam(Boolean permissionEditTeam) {
        this.permissionEditTeam = permissionEditTeam;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VTeamMembershipWithPermissionFilter setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VTeamMembershipWithPermissionFilter setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VTeamMembershipWithPermissionFilter setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VTeamMembershipWithPermissionFilter setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VTeamMembershipWithPermissionFilter setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VTeamMembershipWithPermissionFilter setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public VTeamMembershipWithPermissionFilter setProcessPermissionCreate(Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public VTeamMembershipWithPermissionFilter setProcessPermissionRead(Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public VTeamMembershipWithPermissionFilter setProcessPermissionEdit(Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public VTeamMembershipWithPermissionFilter setProcessPermissionDelete(Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public VTeamMembershipWithPermissionFilter setProcessPermissionAnnotate(Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public VTeamMembershipWithPermissionFilter setProcessPermissionPublish(Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public VTeamMembershipWithPermissionFilter setProcessInstancePermissionCreate(Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public VTeamMembershipWithPermissionFilter setProcessInstancePermissionRead(Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public VTeamMembershipWithPermissionFilter setProcessInstancePermissionEdit(Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public VTeamMembershipWithPermissionFilter setProcessInstancePermissionDelete(Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public VTeamMembershipWithPermissionFilter setProcessInstancePermissionAnnotate(Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }
}
