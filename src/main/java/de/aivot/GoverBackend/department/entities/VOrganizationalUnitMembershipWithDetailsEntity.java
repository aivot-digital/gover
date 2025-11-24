package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "v_organizational_unit_memberships_with_details")
public class VOrganizationalUnitMembershipWithDetailsEntity {
    @Id
    private Integer membershipId;

    @Nonnull
    private Integer organizationalUnitId;
    @Nonnull
    private String organizationalUnitName;
    @Nullable
    private Integer organizationalUnitParentOrgUnitId;
    @Nonnull
    private Integer organizationalUnitDepth;

    @Nonnull
    private String userId;
    @Nonnull
    private String userFirstName;
    @Nonnull
    private String userLastName;
    @Nonnull
    private String userFullName;
    @Nonnull
    private String userEmail;
    @Nonnull
    private Boolean userEnabled;
    @Nonnull
    private Boolean userVerified;
    @Nonnull
    private Boolean userGlobalAdmin;
    @Nonnull
    private Boolean userDeletedInIdp;

    public Integer getMembershipId() {
        return membershipId;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    @Nonnull
    public Integer getOrganizationalUnitId() {
        return organizationalUnitId;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setOrganizationalUnitId(@Nonnull Integer organizationalUnitId) {
        this.organizationalUnitId = organizationalUnitId;
        return this;
    }

    @Nonnull
    public String getOrganizationalUnitName() {
        return organizationalUnitName;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setOrganizationalUnitName(@Nonnull String organizationalUnitName) {
        this.organizationalUnitName = organizationalUnitName;
        return this;
    }

    @Nullable
    public Integer getOrganizationalUnitParentOrgUnitId() {
        return organizationalUnitParentOrgUnitId;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setOrganizationalUnitParentOrgUnitId(@Nullable Integer organizationalUnitParentOrgUnitId) {
        this.organizationalUnitParentOrgUnitId = organizationalUnitParentOrgUnitId;
        return this;
    }

    @Nonnull
    public Integer getOrganizationalUnitDepth() {
        return organizationalUnitDepth;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setOrganizationalUnitDepth(@Nonnull Integer organizationalUnitDepth) {
        this.organizationalUnitDepth = organizationalUnitDepth;
        return this;
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
        return this;
    }

    @Nonnull
    public String getUserFirstName() {
        return userFirstName;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserFirstName(@Nonnull String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    @Nonnull
    public String getUserLastName() {
        return userLastName;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserLastName(@Nonnull String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    @Nonnull
    public String getUserFullName() {
        return userFullName;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserFullName(@Nonnull String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    @Nonnull
    public String getUserEmail() {
        return userEmail;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserEmail(@Nonnull String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    @Nonnull
    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserEnabled(@Nonnull Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    @Nonnull
    public Boolean getUserVerified() {
        return userVerified;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserVerified(@Nonnull Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    @Nonnull
    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserGlobalAdmin(@Nonnull Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    @Nonnull
    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserDeletedInIdp(@Nonnull Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }
}
