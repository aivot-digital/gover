package de.aivot.GoverBackend.resourceAccessControl.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_access_controls")
public class ResourceAccessControlEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "resource_access_controls_id_seq")
    @SequenceGenerator(name = "resource_access_controls_id_seq", allocationSize = 1)
    private Integer id;

    @Nullable
    private Integer sourceTeamId;

    @Nullable
    private Integer sourceDepartmentId;

    @Nullable
    private Integer targetFormId;

    @Nullable
    private Integer targetProcessId;

    @Nullable
    private Integer targetProcessInstanceId;

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

    @Nonnull
    private LocalDateTime created;

    @Nonnull
    private LocalDateTime updated;

    // region Signales

    @PrePersist
    public void prePersist() {
        created = LocalDateTime.now();
        updated = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updated = LocalDateTime.now();
    }

    // endregion

    // region Getters and Setters

    @Nonnull
    public Integer getId() {
        return id;
    }

    public ResourceAccessControlEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nullable
    public Integer getSourceTeamId() {
        return sourceTeamId;
    }

    public ResourceAccessControlEntity setSourceTeamId(@Nullable Integer sourceTeamId) {
        this.sourceTeamId = sourceTeamId;
        return this;
    }

    @Nullable
    public Integer getSourceDepartmentId() {
        return sourceDepartmentId;
    }

    public ResourceAccessControlEntity setSourceDepartmentId(@Nullable Integer sourceOrgUnitId) {
        this.sourceDepartmentId = sourceOrgUnitId;
        return this;
    }

    @Nullable
    public Integer getTargetFormId() {
        return targetFormId;
    }

    public ResourceAccessControlEntity setTargetFormId(@Nullable Integer targetFormId) {
        this.targetFormId = targetFormId;
        return this;
    }

    @Nullable
    public Integer getTargetProcessId() {
        return targetProcessId;
    }

    public ResourceAccessControlEntity setTargetProcessId(@Nullable Integer targetProcessId) {
        this.targetProcessId = targetProcessId;
        return this;
    }

    @Nullable
    public Integer getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public ResourceAccessControlEntity setTargetProcessInstanceId(@Nullable Integer targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public ResourceAccessControlEntity setFormPermissionCreate(@Nonnull Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public ResourceAccessControlEntity setFormPermissionRead(@Nonnull Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public ResourceAccessControlEntity setFormPermissionEdit(@Nonnull Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public ResourceAccessControlEntity setFormPermissionDelete(@Nonnull Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public ResourceAccessControlEntity setFormPermissionAnnotate(@Nonnull Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public ResourceAccessControlEntity setFormPermissionPublish(@Nonnull Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public ResourceAccessControlEntity setProcessPermissionCreate(@Nonnull Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public ResourceAccessControlEntity setProcessPermissionRead(@Nonnull Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public ResourceAccessControlEntity setProcessPermissionEdit(@Nonnull Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public ResourceAccessControlEntity setProcessPermissionDelete(@Nonnull Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public ResourceAccessControlEntity setProcessPermissionAnnotate(@Nonnull Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public ResourceAccessControlEntity setProcessPermissionPublish(@Nonnull Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public ResourceAccessControlEntity setProcessInstancePermissionCreate(@Nonnull Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public ResourceAccessControlEntity setProcessInstancePermissionRead(@Nonnull Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public ResourceAccessControlEntity setProcessInstancePermissionEdit(@Nonnull Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public ResourceAccessControlEntity setProcessInstancePermissionDelete(@Nonnull Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public ResourceAccessControlEntity setProcessInstancePermissionAnnotate(@Nonnull Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public ResourceAccessControlEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public ResourceAccessControlEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }


    // endregion
}
