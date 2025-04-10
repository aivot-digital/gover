package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.MembershipWithUserEntity;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

public class MembershipWithUserFilter implements Filter<MembershipWithUserEntity> {
    private String userId;
    private Integer departmentId;
    private Integer membershipId;
    private String userEmail;
    private String userName;
    private Boolean userEnabled;
    private Boolean userVerified;
    private Boolean userGlobalAdmin;
    private UserRole membershipRole;

    public static MembershipWithUserFilter create() {
        return new MembershipWithUserFilter();
    }

    @Override
    public Specification<MembershipWithUserEntity> build() {
        return SpecificationBuilder
                .create(MembershipWithUserEntity.class)
                .withEquals("userId", userId)
                .withEquals("departmentId", departmentId)
                .withEquals("membershipId", membershipId)
                .withContains("userEmail", userEmail)
                .withContains("userFullName", userName)
                .withEquals("userEnabled", userEnabled)
                .withEquals("userVerified", userVerified)
                .withEquals("userGlobalAdmin", userGlobalAdmin)
                .withEquals("membershipRole", membershipRole != null ? membershipRole.getKey() : null)
                .build();
    }

    public String getUserId() {
        return userId;
    }

    public MembershipWithUserFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public MembershipWithUserFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public Integer getMembershipId() {
        return membershipId;
    }

    public MembershipWithUserFilter setMembershipId(Integer membershipId) {
        this.membershipId = membershipId;
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public MembershipWithUserFilter setUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public String getUserName() {
        return userName;
    }

    public MembershipWithUserFilter setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public Boolean getUserEnabled() {
        return userEnabled;
    }

    public MembershipWithUserFilter setUserEnabled(Boolean userEnabled) {
        this.userEnabled = userEnabled;
        return this;
    }

    public Boolean getUserVerified() {
        return userVerified;
    }

    public MembershipWithUserFilter setUserVerified(Boolean userVerified) {
        this.userVerified = userVerified;
        return this;
    }

    public Boolean getUserGlobalAdmin() {
        return userGlobalAdmin;
    }

    public MembershipWithUserFilter setUserGlobalAdmin(Boolean userGlobalAdmin) {
        this.userGlobalAdmin = userGlobalAdmin;
        return this;
    }

    public UserRole getMembershipRole() {
        return membershipRole;
    }

    public MembershipWithUserFilter setMembershipRole(UserRole membershipRole) {
        this.membershipRole = membershipRole;
        return this;
    }
}
