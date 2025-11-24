package de.aivot.GoverBackend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "v_team_user_role_assignments_with_details")
public class VTeamUserRoleAssignmentsWithDetailsEntity {
    @Id
    private Integer userRoleAssignmentId;

    @Nonnull
    private Integer userRoleId;
    @Nonnull
    private String userRoleName;
    @Nullable
    private String userRoleDescription;
    @Nonnull
    private Boolean userRoleOrgUnitMemberPermissionEdit;
    @Nonnull
    private Boolean userRoleTeamMemberPermissionEdit;
    @Nonnull
    private Boolean userRoleFormPermissionCreate;
    @Nonnull
    private Boolean userRoleFormPermissionRead;
    @Nonnull
    private Boolean userRoleFormPermissionEdit;
    @Nonnull
    private Boolean userRoleFormPermissionDelete;
    @Nonnull
    private Boolean userRoleFormPermissionAnnotate;
    @Nonnull
    private Boolean userRoleFormPermissionPublish;
    @Nonnull
    private Boolean userRoleProcessPermissionCreate;
    @Nonnull
    private Boolean userRoleProcessPermissionRead;
    @Nonnull
    private Boolean userRoleProcessPermissionEdit;
    @Nonnull
    private Boolean userRoleProcessPermissionDelete;
    @Nonnull
    private Boolean userRoleProcessPermissionAnnotate;
    @Nonnull
    private Boolean userRoleProcessPermissionPublish;
    @Nonnull
    private Boolean userRoleProcessInstancePermissionCreate;
    @Nonnull
    private Boolean userRoleProcessInstancePermissionRead;
    @Nonnull
    private Boolean userRoleProcessInstancePermissionEdit;
    @Nonnull
    private Boolean userRoleProcessInstancePermissionDelete;
    @Nonnull
    private Boolean userRoleProcessInstancePermissionAnnotate;

    @Nonnull
    private Integer teamMembershipId;
    @Nonnull
    private Integer teamMembershipTeamId;
    @Nonnull
    private String teamMembershipTeamName;
    @Nonnull
    private String teamMembershipUserId;
    @Nonnull
    private String teamMembershipUserFirstName;
    @Nonnull
    private String teamMembershipUserLastName;
    @Nonnull
    private String teamMembershipUserFullName;
    @Nonnull
    private String teamMembershipUserEmail;
    @Nonnull
    private Boolean teamMembershipUserEnabled;
    @Nonnull
    private Boolean teamMembershipUserVerified;
    @Nonnull
    private Boolean teamMembershipUserGlobalAdmin;
    @Nonnull
    private Boolean teamMembershipUserDeletedInIdp;

    public Integer getUserRoleAssignmentId() {
        return userRoleAssignmentId;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleAssignmentId(Integer userRoleAssignmentId) {
        this.userRoleAssignmentId = userRoleAssignmentId;
        return this;
    }

    @Nonnull
    public Integer getUserRoleId() {
        return userRoleId;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleId(@Nonnull Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }

    @Nonnull
    public String getUserRoleName() {
        return userRoleName;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleName(@Nonnull String userRoleName) {
        this.userRoleName = userRoleName;
        return this;
    }

    @Nullable
    public String getUserRoleDescription() {
        return userRoleDescription;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleDescription(@Nullable String userRoleDescription) {
        this.userRoleDescription = userRoleDescription;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleOrgUnitMemberPermissionEdit() {
        return userRoleOrgUnitMemberPermissionEdit;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleOrgUnitMemberPermissionEdit(@Nonnull Boolean userRoleOrgUnitMemberPermissionEdit) {
        this.userRoleOrgUnitMemberPermissionEdit = userRoleOrgUnitMemberPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleTeamMemberPermissionEdit() {
        return userRoleTeamMemberPermissionEdit;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleTeamMemberPermissionEdit(@Nonnull Boolean userRoleTeamMemberPermissionEdit) {
        this.userRoleTeamMemberPermissionEdit = userRoleTeamMemberPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionCreate() {
        return userRoleFormPermissionCreate;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionCreate(@Nonnull Boolean userRoleFormPermissionCreate) {
        this.userRoleFormPermissionCreate = userRoleFormPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionRead() {
        return userRoleFormPermissionRead;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionRead(@Nonnull Boolean userRoleFormPermissionRead) {
        this.userRoleFormPermissionRead = userRoleFormPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionEdit() {
        return userRoleFormPermissionEdit;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionEdit(@Nonnull Boolean userRoleFormPermissionEdit) {
        this.userRoleFormPermissionEdit = userRoleFormPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionDelete() {
        return userRoleFormPermissionDelete;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionDelete(@Nonnull Boolean userRoleFormPermissionDelete) {
        this.userRoleFormPermissionDelete = userRoleFormPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionAnnotate() {
        return userRoleFormPermissionAnnotate;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionAnnotate(@Nonnull Boolean userRoleFormPermissionAnnotate) {
        this.userRoleFormPermissionAnnotate = userRoleFormPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionPublish() {
        return userRoleFormPermissionPublish;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionPublish(@Nonnull Boolean userRoleFormPermissionPublish) {
        this.userRoleFormPermissionPublish = userRoleFormPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionCreate() {
        return userRoleProcessPermissionCreate;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionCreate(@Nonnull Boolean userRoleProcessPermissionCreate) {
        this.userRoleProcessPermissionCreate = userRoleProcessPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionRead() {
        return userRoleProcessPermissionRead;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionRead(@Nonnull Boolean userRoleProcessPermissionRead) {
        this.userRoleProcessPermissionRead = userRoleProcessPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionEdit() {
        return userRoleProcessPermissionEdit;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionEdit(@Nonnull Boolean userRoleProcessPermissionEdit) {
        this.userRoleProcessPermissionEdit = userRoleProcessPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionDelete() {
        return userRoleProcessPermissionDelete;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionDelete(@Nonnull Boolean userRoleProcessPermissionDelete) {
        this.userRoleProcessPermissionDelete = userRoleProcessPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionAnnotate() {
        return userRoleProcessPermissionAnnotate;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionAnnotate(@Nonnull Boolean userRoleProcessPermissionAnnotate) {
        this.userRoleProcessPermissionAnnotate = userRoleProcessPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionPublish() {
        return userRoleProcessPermissionPublish;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionPublish(@Nonnull Boolean userRoleProcessPermissionPublish) {
        this.userRoleProcessPermissionPublish = userRoleProcessPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionCreate() {
        return userRoleProcessInstancePermissionCreate;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionCreate(@Nonnull Boolean userRoleProcessInstancePermissionCreate) {
        this.userRoleProcessInstancePermissionCreate = userRoleProcessInstancePermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionRead() {
        return userRoleProcessInstancePermissionRead;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionRead(@Nonnull Boolean userRoleProcessInstancePermissionRead) {
        this.userRoleProcessInstancePermissionRead = userRoleProcessInstancePermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionEdit() {
        return userRoleProcessInstancePermissionEdit;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionEdit(@Nonnull Boolean userRoleProcessInstancePermissionEdit) {
        this.userRoleProcessInstancePermissionEdit = userRoleProcessInstancePermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionDelete() {
        return userRoleProcessInstancePermissionDelete;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionDelete(@Nonnull Boolean userRoleProcessInstancePermissionDelete) {
        this.userRoleProcessInstancePermissionDelete = userRoleProcessInstancePermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionAnnotate() {
        return userRoleProcessInstancePermissionAnnotate;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionAnnotate(@Nonnull Boolean userRoleProcessInstancePermissionAnnotate) {
        this.userRoleProcessInstancePermissionAnnotate = userRoleProcessInstancePermissionAnnotate;
        return this;
    }

    @Nonnull
    public Integer getTeamMembershipId() {
        return teamMembershipId;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipId(@Nonnull Integer teamMembershipId) {
        this.teamMembershipId = teamMembershipId;
        return this;
    }

    @Nonnull
    public Integer getTeamMembershipTeamId() {
        return teamMembershipTeamId;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipTeamId(@Nonnull Integer teamMembershipTeamId) {
        this.teamMembershipTeamId = teamMembershipTeamId;
        return this;
    }

    @Nonnull
    public String getTeamMembershipTeamName() {
        return teamMembershipTeamName;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipTeamName(@Nonnull String teamMembershipTeamName) {
        this.teamMembershipTeamName = teamMembershipTeamName;
        return this;
    }

    @Nonnull
    public String getTeamMembershipUserId() {
        return teamMembershipUserId;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserId(@Nonnull String teamMembershipUserId) {
        this.teamMembershipUserId = teamMembershipUserId;
        return this;
    }

    @Nonnull
    public String getTeamMembershipUserFirstName() {
        return teamMembershipUserFirstName;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserFirstName(@Nonnull String teamMembershipUserFirstName) {
        this.teamMembershipUserFirstName = teamMembershipUserFirstName;
        return this;
    }

    @Nonnull
    public String getTeamMembershipUserLastName() {
        return teamMembershipUserLastName;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserLastName(@Nonnull String teamMembershipUserLastName) {
        this.teamMembershipUserLastName = teamMembershipUserLastName;
        return this;
    }

    @Nonnull
    public String getTeamMembershipUserFullName() {
        return teamMembershipUserFullName;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserFullName(@Nonnull String teamMembershipUserFullName) {
        this.teamMembershipUserFullName = teamMembershipUserFullName;
        return this;
    }

    @Nonnull
    public String getTeamMembershipUserEmail() {
        return teamMembershipUserEmail;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserEmail(@Nonnull String teamMembershipUserEmail) {
        this.teamMembershipUserEmail = teamMembershipUserEmail;
        return this;
    }

    @Nonnull
    public Boolean getTeamMembershipUserEnabled() {
        return teamMembershipUserEnabled;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserEnabled(@Nonnull Boolean teamMembershipUserEnabled) {
        this.teamMembershipUserEnabled = teamMembershipUserEnabled;
        return this;
    }

    @Nonnull
    public Boolean getTeamMembershipUserVerified() {
        return teamMembershipUserVerified;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserVerified(@Nonnull Boolean teamMembershipUserVerified) {
        this.teamMembershipUserVerified = teamMembershipUserVerified;
        return this;
    }

    @Nonnull
    public Boolean getTeamMembershipUserGlobalAdmin() {
        return teamMembershipUserGlobalAdmin;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserGlobalAdmin(@Nonnull Boolean teamMembershipUserGlobalAdmin) {
        this.teamMembershipUserGlobalAdmin = teamMembershipUserGlobalAdmin;
        return this;
    }

    @Nonnull
    public Boolean getTeamMembershipUserDeletedInIdp() {
        return teamMembershipUserDeletedInIdp;
    }

    public VTeamUserRoleAssignmentsWithDetailsEntity setTeamMembershipUserDeletedInIdp(@Nonnull Boolean teamMembershipUserDeletedInIdp) {
        this.teamMembershipUserDeletedInIdp = teamMembershipUserDeletedInIdp;
        return this;
    }
}