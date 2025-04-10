package de.aivot.GoverBackend.department.entities;

import de.aivot.GoverBackend.enums.UserRole;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "memberships_with_users")
@IdClass(MembershipWithUserEntityId.class)
public class MembershipWithUserEntity {
    @Id
    private Integer id;
    @Id
    private Integer departmentId;
    private UserRole role;
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

    public MembershipWithUserEntity setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public MembershipWithUserEntity setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public MembershipWithUserEntity setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public MembershipWithUserEntity setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public MembershipWithUserEntity setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserFirstName() {
        return userFirstName;
    }

    public MembershipWithUserEntity setUserFirstName(String userFirstName) {
        this.userFirstName = userFirstName;
        return this;
    }

    public String getUserLastName() {
        return userLastName;
    }

    public MembershipWithUserEntity setUserLastName(String userLastName) {
        this.userLastName = userLastName;
        return this;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public MembershipWithUserEntity setUserFullName(String userFullName) {
        this.userFullName = userFullName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public MembershipWithUserEntity setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public MembershipWithUserEntity setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public MembershipWithUserEntity setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public MembershipWithUserEntity setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    // endregion
}
