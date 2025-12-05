package de.aivot.GoverBackend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_roles_id_seq")
    @SequenceGenerator(name = "user_roles_id_seq", allocationSize = 1)
    private Integer id;

    @Nonnull
    @Column(length = 64, nullable = false, unique = true)
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

    @Nonnull
    private Boolean departmentPermissionEdit;

    @Nonnull
    private Boolean teamPermissionEdit;

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
    @Column(nullable = false)
    private LocalDateTime created;

    @Nonnull
    @Column(nullable = false)
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

    public UserRoleEntity setId(@Nonnull Integer id) {
        this.id = id;
        return this;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public UserRoleEntity setName(@Nonnull String name) {
        this.name = name;
        return this;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public UserRoleEntity setDescription(@Nullable String description) {
        this.description = description;
        return this;
    }

    @Nonnull
    public Boolean getDepartmentPermissionEdit() {
        return departmentPermissionEdit;
    }

    public UserRoleEntity setDepartmentPermissionEdit(@Nonnull Boolean permissionEditMemberOfEntity) {
        this.departmentPermissionEdit = permissionEditMemberOfEntity;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public UserRoleEntity setFormPermissionCreate(@Nonnull Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public UserRoleEntity setFormPermissionRead(@Nonnull Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public UserRoleEntity setFormPermissionEdit(@Nonnull Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public UserRoleEntity setFormPermissionDelete(@Nonnull Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public UserRoleEntity setFormPermissionAnnotate(@Nonnull Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public UserRoleEntity setFormPermissionPublish(@Nonnull Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public UserRoleEntity setProcessPermissionCreate(@Nonnull Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public UserRoleEntity setProcessPermissionRead(@Nonnull Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public UserRoleEntity setProcessPermissionEdit(@Nonnull Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public UserRoleEntity setProcessPermissionDelete(@Nonnull Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public UserRoleEntity setProcessPermissionAnnotate(@Nonnull Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public UserRoleEntity setProcessPermissionPublish(@Nonnull Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public UserRoleEntity setProcessInstancePermissionCreate(@Nonnull Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public UserRoleEntity setProcessInstancePermissionRead(@Nonnull Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public UserRoleEntity setProcessInstancePermissionEdit(@Nonnull Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public UserRoleEntity setProcessInstancePermissionDelete(@Nonnull Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public UserRoleEntity setProcessInstancePermissionAnnotate(@Nonnull Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }

    @Nonnull
    public LocalDateTime getCreated() {
        return created;
    }

    public UserRoleEntity setCreated(@Nonnull LocalDateTime created) {
        this.created = created;
        return this;
    }

    @Nonnull
    public LocalDateTime getUpdated() {
        return updated;
    }

    public UserRoleEntity setUpdated(@Nonnull LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    @Nonnull
    public Boolean getTeamPermissionEdit() {
        return teamPermissionEdit;
    }

    public UserRoleEntity setTeamPermissionEdit(@Nonnull Boolean teamMemberPermissionEdit) {
        this.teamPermissionEdit = teamMemberPermissionEdit;
        return this;
    }


    // endregion
}
