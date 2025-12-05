package de.aivot.GoverBackend.teams.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "v_team_user_role_assignments_with_details")
public class VTeamUserRoleAssignmentWithDetailsEntity {
    private Integer id;
    private Integer teamId;
    private String userId;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String name;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Boolean enabled;
    private Boolean verified;
    private Integer globalRole;
    private Boolean deletedInIdp;
    @Id
    private Integer userRoleAssignmentId;
    private Integer userRoleId;
    private String userRoleName;
    private String description;
    private Boolean departmentPermissionEdit;
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

    public VTeamUserRoleAssignmentWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setTeamId(Integer teamId) {
        this.teamId = teamId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public String getName() {
        return name;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Integer getGlobalRole() {
        return globalRole;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setGlobalRole(Integer globalAdmin) {
        this.globalRole = globalAdmin;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }

    public Integer getUserRoleAssignmentId() {
        return userRoleAssignmentId;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setUserRoleAssignmentId(Integer userRoleAssignmentId) {
        this.userRoleAssignmentId = userRoleAssignmentId;
        return this;
    }

    public String getUserRoleName() {
        return userRoleName;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setUserRoleName(String userRoleName) {
        this.userRoleName = userRoleName;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public Boolean getDepartmentPermissionEdit() {
        return departmentPermissionEdit;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setDepartmentPermissionEdit(Boolean departmentPermissionEdit) {
        this.departmentPermissionEdit = departmentPermissionEdit;
        return this;
    }

    public Boolean getTeamPermissionEdit() {
        return teamPermissionEdit;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setTeamPermissionEdit(Boolean teamPermissionEdit) {
        this.teamPermissionEdit = teamPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionCreate() {
        return formPermissionCreate;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public Boolean getFormPermissionRead() {
        return formPermissionRead;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFormPermissionRead(Boolean formPermissionRead) {
        this.formPermissionRead = formPermissionRead;
        return this;
    }

    public Boolean getFormPermissionEdit() {
        return formPermissionEdit;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }

    public Boolean getFormPermissionDelete() {
        return formPermissionDelete;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFormPermissionDelete(Boolean formPermissionDelete) {
        this.formPermissionDelete = formPermissionDelete;
        return this;
    }

    public Boolean getFormPermissionAnnotate() {
        return formPermissionAnnotate;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFormPermissionAnnotate(Boolean formPermissionAnnotate) {
        this.formPermissionAnnotate = formPermissionAnnotate;
        return this;
    }

    public Boolean getFormPermissionPublish() {
        return formPermissionPublish;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setFormPermissionPublish(Boolean formPermissionPublish) {
        this.formPermissionPublish = formPermissionPublish;
        return this;
    }

    public Boolean getProcessPermissionCreate() {
        return processPermissionCreate;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessPermissionCreate(Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    public Boolean getProcessPermissionRead() {
        return processPermissionRead;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessPermissionRead(Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }

    public Boolean getProcessPermissionEdit() {
        return processPermissionEdit;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessPermissionEdit(Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    public Boolean getProcessPermissionDelete() {
        return processPermissionDelete;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessPermissionDelete(Boolean processPermissionDelete) {
        this.processPermissionDelete = processPermissionDelete;
        return this;
    }

    public Boolean getProcessPermissionAnnotate() {
        return processPermissionAnnotate;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessPermissionAnnotate(Boolean processPermissionAnnotate) {
        this.processPermissionAnnotate = processPermissionAnnotate;
        return this;
    }

    public Boolean getProcessPermissionPublish() {
        return processPermissionPublish;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessPermissionPublish(Boolean processPermissionPublish) {
        this.processPermissionPublish = processPermissionPublish;
        return this;
    }

    public Boolean getProcessInstancePermissionCreate() {
        return processInstancePermissionCreate;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionCreate(Boolean processInstancePermissionCreate) {
        this.processInstancePermissionCreate = processInstancePermissionCreate;
        return this;
    }

    public Boolean getProcessInstancePermissionRead() {
        return processInstancePermissionRead;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionRead(Boolean processInstancePermissionRead) {
        this.processInstancePermissionRead = processInstancePermissionRead;
        return this;
    }

    public Boolean getProcessInstancePermissionEdit() {
        return processInstancePermissionEdit;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionEdit(Boolean processInstancePermissionEdit) {
        this.processInstancePermissionEdit = processInstancePermissionEdit;
        return this;
    }

    public Boolean getProcessInstancePermissionDelete() {
        return processInstancePermissionDelete;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionDelete(Boolean processInstancePermissionDelete) {
        this.processInstancePermissionDelete = processInstancePermissionDelete;
        return this;
    }

    public Boolean getProcessInstancePermissionAnnotate() {
        return processInstancePermissionAnnotate;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setProcessInstancePermissionAnnotate(Boolean processInstancePermissionAnnotate) {
        this.processInstancePermissionAnnotate = processInstancePermissionAnnotate;
        return this;
    }

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public VTeamUserRoleAssignmentWithDetailsEntity setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }
}
