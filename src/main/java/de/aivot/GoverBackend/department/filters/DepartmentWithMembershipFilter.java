package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntity;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class DepartmentWithMembershipFilter implements Filter<DepartmentWithMembershipEntity> {
    private String userId;
    private Integer departmentId;
    private List<Integer> departmentIds;
    private Integer membershipId;
    private String departmentName;
    private String userEmail;
    private String userName;
    private Boolean userEnabled;
    private Boolean userDeletedInIdp;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private UserRole membershipRole;
    private Boolean ignoreMemberships;
    private Boolean deletedInIdp;

    public static DepartmentWithMembershipFilter create() {
        return new DepartmentWithMembershipFilter();
    }

    @Override
    public Specification<DepartmentWithMembershipEntity> build() {
        return SpecificationBuilder
                .create(DepartmentWithMembershipEntity.class)
                .withEquals("id", departmentId)
                .withInList("id", departmentIds)
                .withContains("name", departmentName)
                .withEquals("userId", userId)
                .withEquals("membershipId", membershipId)
                .withContains("userEmail", userEmail)
                .withContains("userFullName", userName)
                .withEquals("userEnabled", userEnabled)
                .withEquals("userDeletedInIdp", userDeletedInIdp)
                .withEquals("userVerified", userVerified)
                .withEquals("userGlobalAdmin", userGlobalAdmin)
                .withEquals("membershipRole", membershipRole != null ? membershipRole.getKey() : null)
                .withEquals("userDeletedInIdp", deletedInIdp)
                .build();
    }

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public DepartmentWithMembershipFilter setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public DepartmentWithMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public DepartmentWithMembershipFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public Integer getMembershipId() {
        return membershipId;
    }

    public DepartmentWithMembershipFilter setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public DepartmentWithMembershipFilter setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public DepartmentWithMembershipFilter setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public DepartmentWithMembershipFilter setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public DepartmentWithMembershipFilter setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public DepartmentWithMembershipFilter setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public DepartmentWithMembershipFilter setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public UserRole getMembershipRole() {
        return membershipRole;
    }

    public DepartmentWithMembershipFilter setMembershipRole(UserRole membershipRole) {
        this.membershipRole = membershipRole;
        return this;
    }

    public Boolean getIgnoreMemberships() {
        return ignoreMemberships;
    }

    public DepartmentWithMembershipFilter setIgnoreMemberships(Boolean ignoreMemberships) {
        this.ignoreMemberships = ignoreMemberships;
        return this;
    }

    public Boolean getUserDeletedInIdp() {
        return userDeletedInIdp;
    }

    public DepartmentWithMembershipFilter setUserDeletedInIdp(Boolean userDeletedInIdp) {
        this.userDeletedInIdp = userDeletedInIdp;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public DepartmentWithMembershipFilter setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }
}
