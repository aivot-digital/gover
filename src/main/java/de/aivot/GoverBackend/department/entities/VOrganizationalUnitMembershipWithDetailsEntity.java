package de.aivot.GoverBackend.department.entities;

import jakarta.annotation.Nonnull;
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

    @Nonnull
    private String userId;
    @Nonnull
    private String userFullName;
    @Nonnull
    private String userEmail;

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

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public VOrganizationalUnitMembershipWithDetailsEntity setUserId(@Nonnull String userId) {
        this.userId = userId;
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
}
