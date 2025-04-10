package de.aivot.GoverBackend.department.entities;

import de.aivot.GoverBackend.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments_with_memberships")
@IdClass(DepartmentWithMembershipEntityId.class)
public class DepartmentWithMembershipEntity {
    @Id
    private Integer id;
    private String name;
    private String address;
    private String imprint;
    private String privacy;
    private String accessibility;
    private String technicalSupportAddress;
    private String specialSupportAddress;
    private LocalDateTime created;
    private LocalDateTime updated;
    private String departmentMail;
    @Id
    private Integer membershipId;
    private UserRole membershipRole;
    @Id
    private String userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private String userFullName;
    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private Boolean userDeletedInIdp;

    // region Getters and Setters

    public Integer getId() {
        return id;
    }

    public DepartmentWithMembershipEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public DepartmentWithMembershipEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public DepartmentWithMembershipEntity setAddress(String address) {
        this.address = address;
        return this;
    }

    public String getImprint() {
        return imprint;
    }

    public DepartmentWithMembershipEntity setImprint(String imprint) {
        this.imprint = imprint;
        return this;
    }

    public String getPrivacy() {
        return privacy;
    }

    public DepartmentWithMembershipEntity setPrivacy(String privacy) {
        this.privacy = privacy;
        return this;
    }

    public String getAccessibility() {
        return accessibility;
    }

    public DepartmentWithMembershipEntity setAccessibility(String accessibility) {
        this.accessibility = accessibility;
        return this;
    }

    public String getTechnicalSupportAddress() {
        return technicalSupportAddress;
    }

    public DepartmentWithMembershipEntity setTechnicalSupportAddress(String technicalSupportAddress) {
        this.technicalSupportAddress = technicalSupportAddress;
        return this;
    }

    public String getSpecialSupportAddress() {
        return specialSupportAddress;
    }

    public DepartmentWithMembershipEntity setSpecialSupportAddress(String specialSupportAddress) {
        this.specialSupportAddress = specialSupportAddress;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public DepartmentWithMembershipEntity setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public DepartmentWithMembershipEntity setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public String getDepartmentMail() {
        return departmentMail;
    }

    public DepartmentWithMembershipEntity setDepartmentMail(String departmentMail) {
        this.departmentMail = departmentMail;
        return this;
    }

    public Integer getMembershipId() {
        return membershipId;
    }

    public DepartmentWithMembershipEntity setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public UserRole getMembershipRole() {
        return membershipRole;
    }

    public DepartmentWithMembershipEntity setMembershipRole(UserRole membershipRole) {
        this.membershipRole = membershipRole;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public DepartmentWithMembershipEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public DepartmentWithMembershipEntity setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public DepartmentWithMembershipEntity setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public DepartmentWithMembershipEntity setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public DepartmentWithMembershipEntity setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public DepartmentWithMembershipEntity setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public DepartmentWithMembershipEntity setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public DepartmentWithMembershipEntity setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public DepartmentWithMembershipEntity setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    // endregion
}
