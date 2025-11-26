package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_department_memberships_with_permissions")
public class VDepartmentMembershipWithPermissionsEntity {
    @Id
    private Integer id;
    @Nonnull
    private Integer departmentId;
    @Nonnull
    private String userId;
    @Nonnull
    private LocalDateTime created;
    @Nonnull
    private LocalDateTime updated;
    @Nonnull
    private Boolean departmentPermissionEdit;
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

    public Integer getId() {
        return id;
    }

    public VDepartmentMembershipWithPermissionsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithPermissionsEntity setDepartmentId(@Nonnull Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithPermissionsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public VDepartmentMembershipWithPermissionsEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public VDepartmentMembershipWithPermissionsEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public Boolean getDepartmentPermissionEdit() {
        return departmentPermissionEdit;
    }

    public VDepartmentMembershipWithPermissionsEntity setDepartmentPermissionEdit(@Nonnull Boolean orgUnitMemberPermissionEdit) {
        this.departmentPermissionEdit = orgUnitMemberPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VDepartmentMembershipWithPermissionsEntity setFormPermissionCreate(@Nonnull Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VDepartmentMembershipWithPermissionsEntity setFormPermissionRead(@Nonnull Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VDepartmentMembershipWithPermissionsEntity setFormPermissionEdit(@Nonnull Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VDepartmentMembershipWithPermissionsEntity setFormPermissionDelete(@Nonnull Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VDepartmentMembershipWithPermissionsEntity setFormPermissionAnnotate(@Nonnull Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VDepartmentMembershipWithPermissionsEntity setFormPermissionPublish(@Nonnull Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessPermissionCreate(@Nonnull Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessPermissionRead(@Nonnull Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessPermissionEdit(@Nonnull Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessPermissionDelete(@Nonnull Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessPermissionAnnotate(@Nonnull Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessPermissionPublish(@Nonnull Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessInstancePermissionCreate(@Nonnull Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessInstancePermissionRead(@Nonnull Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessInstancePermissionEdit(@Nonnull Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessInstancePermissionDelete(@Nonnull Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public VDepartmentMembershipWithPermissionsEntity setProcessInstancePermissionAnnotate(@Nonnull Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }


    // endregion
}
