package de.aivot.GoverBackend.userRoles.entities;

import de.aivot.GoverBackend.userRoles.enums.DepartmentPermission;
import de.aivot.GoverBackend.userRoles.enums.FormRolePermission;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {
    @Id
    @Nonnull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nonnull
    @Column(length = 64, nullable = false, unique = true)
    private String name;

    @Nullable
    @Column(columnDefinition = "TEXT")
    private String description;

    @Nonnull
    @Column(nullable = false)
    private LocalDateTime created;

    @Nonnull
    @Column(nullable = false)
    private LocalDateTime updated;

    @Nonnull
    private DepartmentPermission departmentPermission;

    @Nonnull
    private FormRolePermission formPermission;

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
    public DepartmentPermission getDepartmentPermission() {
        return departmentPermission;
    }

    public UserRoleEntity setDepartmentPermission(@Nonnull DepartmentPermission departmentPermission) {
        this.departmentPermission = departmentPermission;
        return this;
    }

    @Nonnull
    public FormRolePermission getFormPermission() {
        return formPermission;
    }

    public UserRoleEntity setFormPermission(@Nonnull FormRolePermission formPermission) {
        this.formPermission = formPermission;
        return this;
    }

    // endregion
}
