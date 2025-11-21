package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "v_organizational_unit_memberships_with_permissions")
public class VOrganizationalUnitMembershipWithPermissionsEntity {
    @Id
    private Integer membershipId;
    @Nonnull
    private Integer organizationalUnitId;
    @Nonnull
    private String userId;
    @Nonnull
    private Boolean orgUnitMemberPermissionEdit;
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

    // region Getters & Setters

    public Integer getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
    }

    @Nonnull
    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public void setOrganizationalUnitId(@Nonnull Integer organizationalUnitId) {
        this.organizationalUnitId = organizationalUnitId;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nonnull String userId) {
        this.userId = userId;
    }

    @Nonnull
    public Boolean getOrgUnitMemberPermissionEdit() {
        return orgUnitMemberPermissionEdit;
    }

    public void setOrgUnitMemberPermissionEdit(@Nonnull Boolean permissionEditOrgUnit) {
        this.orgUnitMemberPermissionEdit = permissionEditOrgUnit;
    }

    @Nonnull
    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public void setFormPermissionCreate(@Nonnull Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
    }

    @Nonnull
    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public void setFormPermissionRead(@Nonnull Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
    }

    @Nonnull
    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public void setFormPermissionEdit(@Nonnull Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
    }

    @Nonnull
    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public void setFormPermissionDelete(@Nonnull Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
    }

    @Nonnull
    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public void setFormPermissionAnnotate(@Nonnull Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
    }

    @Nonnull
    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public void setFormPermissionPublish(@Nonnull Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
    }

    @Nonnull
    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public void setProcessPermissionCreate(@Nonnull Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
    }

    @Nonnull
    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public void setProcessPermissionRead(@Nonnull Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
    }

    @Nonnull
    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public void setProcessPermissionEdit(@Nonnull Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
    }

    @Nonnull
    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public void setProcessPermissionDelete(@Nonnull Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
    }

    @Nonnull
    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public void setProcessPermissionAnnotate(@Nonnull Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
    }

    @Nonnull
    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public void setProcessPermissionPublish(@Nonnull Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public void setProcessInstancePermissionCreate(@Nonnull Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public void setProcessInstancePermissionRead(@Nonnull Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public void setProcessInstancePermissionEdit(@Nonnull Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public void setProcessInstancePermissionDelete(@Nonnull Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public void setProcessInstancePermissionAnnotate(@Nonnull Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
    }

    // endregion
}
