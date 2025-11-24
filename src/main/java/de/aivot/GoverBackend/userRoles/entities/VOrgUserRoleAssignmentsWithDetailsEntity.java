package de.aivot.GoverBackend.userRoles.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "v_org_user_role_assignments_with_details")
public class VOrgUserRoleAssignmentsWithDetailsEntity {
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
    private Integer orgUnitMembershipId;
    @Nonnull
    private Integer orgUnitMembershipOrganizationalUnitId;
    @Nonnull
    private String orgUnitMembershipOrganizationalUnitName;
    @Nullable
    private Integer orgUnitMembershipOrganizationalUnitParentOrgUnitId;
    @Nonnull
    private Integer orgUnitMembershipOrganizationalUnitDepth;
    @Nonnull
    private String orgUnitMembershipUserId;
    @Nonnull
    private String orgUnitMembershipUserFirstName;
    @Nonnull
    private String orgUnitMembershipUserLastName;
    @Nonnull
    private String orgUnitMembershipUserFullName;
    @Nonnull
    private String orgUnitMembershipUserEmail;
    @Nonnull
    private Boolean orgUnitMembershipUserEnabled;
    @Nonnull
    private Boolean orgUnitMembershipUserVerified;
    @Nonnull
    private Boolean orgUnitMembershipUserGlobalAdmin;
    @Nonnull
    private Boolean orgUnitMembershipUserDeletedInIdp;

    public Integer getUserRoleAssignmentId() {
        return userRoleAssignmentId;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleAssignmentId(Integer userRoleAssignmentId) {
        this.userRoleAssignmentId = userRoleAssignmentId;
        return this;
    }

    @Nonnull
    public Integer getUserRoleId() {
        return userRoleId;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleId(@Nonnull Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }

    @Nonnull
    public String getUserRoleName() {
        return userRoleName;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleName(@Nonnull String userRoleName) {
        this.userRoleName = userRoleName;
        return this;
    }

    @Nullable
    public String getUserRoleDescription() {
        return userRoleDescription;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleDescription(@Nullable String userRoleDescription) {
        this.userRoleDescription = userRoleDescription;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleOrgUnitMemberPermissionEdit() {
        return userRoleOrgUnitMemberPermissionEdit;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleOrgUnitMemberPermissionEdit(@Nonnull Boolean userRoleOrgUnitMemberPermissionEdit) {
        this.userRoleOrgUnitMemberPermissionEdit = userRoleOrgUnitMemberPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleTeamMemberPermissionEdit() {
        return userRoleTeamMemberPermissionEdit;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleTeamMemberPermissionEdit(@Nonnull Boolean userRoleTeamMemberPermissionEdit) {
        this.userRoleTeamMemberPermissionEdit = userRoleTeamMemberPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionCreate() {
        return userRoleFormPermissionCreate;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionCreate(@Nonnull Boolean userRoleFormPermissionCreate) {
        this.userRoleFormPermissionCreate = userRoleFormPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionRead() {
        return userRoleFormPermissionRead;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionRead(@Nonnull Boolean userRoleFormPermissionRead) {
        this.userRoleFormPermissionRead = userRoleFormPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionEdit() {
        return userRoleFormPermissionEdit;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionEdit(@Nonnull Boolean userRoleFormPermissionEdit) {
        this.userRoleFormPermissionEdit = userRoleFormPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionDelete() {
        return userRoleFormPermissionDelete;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionDelete(@Nonnull Boolean userRoleFormPermissionDelete) {
        this.userRoleFormPermissionDelete = userRoleFormPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionAnnotate() {
        return userRoleFormPermissionAnnotate;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionAnnotate(@Nonnull Boolean userRoleFormPermissionAnnotate) {
        this.userRoleFormPermissionAnnotate = userRoleFormPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleFormPermissionPublish() {
        return userRoleFormPermissionPublish;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleFormPermissionPublish(@Nonnull Boolean userRoleFormPermissionPublish) {
        this.userRoleFormPermissionPublish = userRoleFormPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionCreate() {
        return userRoleProcessPermissionCreate;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionCreate(@Nonnull Boolean userRoleProcessPermissionCreate) {
        this.userRoleProcessPermissionCreate = userRoleProcessPermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionRead() {
        return userRoleProcessPermissionRead;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionRead(@Nonnull Boolean userRoleProcessPermissionRead) {
        this.userRoleProcessPermissionRead = userRoleProcessPermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionEdit() {
        return userRoleProcessPermissionEdit;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionEdit(@Nonnull Boolean userRoleProcessPermissionEdit) {
        this.userRoleProcessPermissionEdit = userRoleProcessPermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionDelete() {
        return userRoleProcessPermissionDelete;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionDelete(@Nonnull Boolean userRoleProcessPermissionDelete) {
        this.userRoleProcessPermissionDelete = userRoleProcessPermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionAnnotate() {
        return userRoleProcessPermissionAnnotate;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionAnnotate(@Nonnull Boolean userRoleProcessPermissionAnnotate) {
        this.userRoleProcessPermissionAnnotate = userRoleProcessPermissionAnnotate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessPermissionPublish() {
        return userRoleProcessPermissionPublish;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessPermissionPublish(@Nonnull Boolean userRoleProcessPermissionPublish) {
        this.userRoleProcessPermissionPublish = userRoleProcessPermissionPublish;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionCreate() {
        return userRoleProcessInstancePermissionCreate;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionCreate(@Nonnull Boolean userRoleProcessInstancePermissionCreate) {
        this.userRoleProcessInstancePermissionCreate = userRoleProcessInstancePermissionCreate;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionRead() {
        return userRoleProcessInstancePermissionRead;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionRead(@Nonnull Boolean userRoleProcessInstancePermissionRead) {
        this.userRoleProcessInstancePermissionRead = userRoleProcessInstancePermissionRead;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionEdit() {
        return userRoleProcessInstancePermissionEdit;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionEdit(@Nonnull Boolean userRoleProcessInstancePermissionEdit) {
        this.userRoleProcessInstancePermissionEdit = userRoleProcessInstancePermissionEdit;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionDelete() {
        return userRoleProcessInstancePermissionDelete;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionDelete(@Nonnull Boolean userRoleProcessInstancePermissionDelete) {
        this.userRoleProcessInstancePermissionDelete = userRoleProcessInstancePermissionDelete;
        return this;
    }

    @Nonnull
    public Boolean getUserRoleProcessInstancePermissionAnnotate() {
        return userRoleProcessInstancePermissionAnnotate;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setUserRoleProcessInstancePermissionAnnotate(@Nonnull Boolean userRoleProcessInstancePermissionAnnotate) {
        this.userRoleProcessInstancePermissionAnnotate = userRoleProcessInstancePermissionAnnotate;
        return this;
    }

    @Nonnull
    public Integer getOrgUnitMembershipId() {
        return orgUnitMembershipId;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipId(@Nonnull Integer orgUnitMembershipId) {
        this.orgUnitMembershipId = orgUnitMembershipId;
        return this;
    }

    @Nonnull
    public Integer getOrgUnitMembershipOrganizationalUnitId() {
        return orgUnitMembershipOrganizationalUnitId;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipOrganizationalUnitId(@Nonnull Integer orgUnitMembershipOrganizationalUnitId) {
        this.orgUnitMembershipOrganizationalUnitId = orgUnitMembershipOrganizationalUnitId;
        return this;
    }

    @Nonnull
    public String getOrgUnitMembershipOrganizationalUnitName() {
        return orgUnitMembershipOrganizationalUnitName;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipOrganizationalUnitName(@Nonnull String orgUnitMembershipOrganizationalUnitName) {
        this.orgUnitMembershipOrganizationalUnitName = orgUnitMembershipOrganizationalUnitName;
        return this;
    }

    @Nullable
    public Integer getOrgUnitMembershipOrganizationalUnitParentOrgUnitId() {
        return orgUnitMembershipOrganizationalUnitParentOrgUnitId;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipOrganizationalUnitParentOrgUnitId(@Nullable Integer orgUnitMembershipOrganizationalUnitParentOrgUnitId) {
        this.orgUnitMembershipOrganizationalUnitParentOrgUnitId = orgUnitMembershipOrganizationalUnitParentOrgUnitId;
        return this;
    }

    @Nonnull
    public Integer getOrgUnitMembershipOrganizationalUnitDepth() {
        return orgUnitMembershipOrganizationalUnitDepth;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipOrganizationalUnitDepth(@Nonnull Integer orgUnitMembershipOrganizationalUnitDepth) {
        this.orgUnitMembershipOrganizationalUnitDepth = orgUnitMembershipOrganizationalUnitDepth;
        return this;
    }

    @Nonnull
    public String getOrgUnitMembershipUserId() {
        return orgUnitMembershipUserId;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserId(@Nonnull String orgUnitMembershipUserId) {
        this.orgUnitMembershipUserId = orgUnitMembershipUserId;
        return this;
    }

    @Nonnull
    public String getOrgUnitMembershipUserFirstName() {
        return orgUnitMembershipUserFirstName;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserFirstName(@Nonnull String orgUnitMembershipUserFirstName) {
        this.orgUnitMembershipUserFirstName = orgUnitMembershipUserFirstName;
        return this;
    }

    @Nonnull
    public String getOrgUnitMembershipUserLastName() {
        return orgUnitMembershipUserLastName;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserLastName(@Nonnull String orgUnitMembershipUserLastName) {
        this.orgUnitMembershipUserLastName = orgUnitMembershipUserLastName;
        return this;
    }

    @Nonnull
    public String getOrgUnitMembershipUserFullName() {
        return orgUnitMembershipUserFullName;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserFullName(@Nonnull String orgUnitMembershipUserFullName) {
        this.orgUnitMembershipUserFullName = orgUnitMembershipUserFullName;
        return this;
    }

    @Nonnull
    public String getOrgUnitMembershipUserEmail() {
        return orgUnitMembershipUserEmail;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserEmail(@Nonnull String orgUnitMembershipUserEmail) {
        this.orgUnitMembershipUserEmail = orgUnitMembershipUserEmail;
        return this;
    }

    @Nonnull
    public Boolean getOrgUnitMembershipUserEnabled() {
        return orgUnitMembershipUserEnabled;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserEnabled(@Nonnull Boolean orgUnitMembershipUserEnabled) {
        this.orgUnitMembershipUserEnabled = orgUnitMembershipUserEnabled;
        return this;
    }

    @Nonnull
    public Boolean getOrgUnitMembershipUserVerified() {
        return orgUnitMembershipUserVerified;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserVerified(@Nonnull Boolean orgUnitMembershipUserVerified) {
        this.orgUnitMembershipUserVerified = orgUnitMembershipUserVerified;
        return this;
    }

    @Nonnull
    public Boolean getOrgUnitMembershipUserGlobalAdmin() {
        return orgUnitMembershipUserGlobalAdmin;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserGlobalAdmin(@Nonnull Boolean orgUnitMembershipUserGlobalAdmin) {
        this.orgUnitMembershipUserGlobalAdmin = orgUnitMembershipUserGlobalAdmin;
        return this;
    }

    @Nonnull
    public Boolean getOrgUnitMembershipUserDeletedInIdp() {
        return orgUnitMembershipUserDeletedInIdp;
    }

    public VOrgUserRoleAssignmentsWithDetailsEntity setOrgUnitMembershipUserDeletedInIdp(@Nonnull Boolean orgUnitMembershipUserDeletedInIdp) {
        this.orgUnitMembershipUserDeletedInIdp = orgUnitMembershipUserDeletedInIdp;
        return this;
    }
}