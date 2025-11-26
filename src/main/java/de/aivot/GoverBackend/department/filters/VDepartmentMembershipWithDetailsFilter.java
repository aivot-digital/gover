package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithDetailsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class VDepartmentMembershipWithDetailsFilter implements Filter<VDepartmentMembershipWithDetailsEntity> {
    private Integer departmentId;
    private List<Integer> departmentIds;
    private String name;
    private String userId;
    private List<String> userIds;
    private String fullName;
    private String email;
    private Boolean enabled;
    private Boolean verified;
    private Boolean globalAdmin;
    private Boolean deletedInIdp;

    public static VDepartmentMembershipWithDetailsFilter create() {
        return new VDepartmentMembershipWithDetailsFilter();
    }

    @Override
    public Specification<VDepartmentMembershipWithDetailsEntity> build() {
        var builder = SpecificationBuilder
                .create(VDepartmentMembershipWithDetailsEntity.class)
                .withEquals("departmentId", departmentId)
                .withInList("departmentId", departmentIds)
                .withContains("name", name)
                .withEquals("userId", userId)
                .withInList("userId", userIds)
                .withContains("fullName", fullName)
                .withContains("email", email);

        if (enabled != null) {
            builder = builder
                    .withEquals("enabled", enabled);
        }

        if (verified != null) {
            builder = builder
                    .withEquals("verified", verified);
        }

        if (globalAdmin != null) {
            builder = builder
                    .withEquals("globalAdmin", globalAdmin);
        }

        if (deletedInIdp != null) {
            builder = builder
                    .withEquals("deletedInIdp", deletedInIdp);
        }

        return builder
                .build();
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithDetailsFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public VDepartmentMembershipWithDetailsFilter setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
        return this;
    }

    public String getName() {
        return name;
    }

    public VDepartmentMembershipWithDetailsFilter setName(String name) {
        this.name = name;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithDetailsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public VDepartmentMembershipWithDetailsFilter setUserIds(List<String> userIds) {
        this.userIds = userIds;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public VDepartmentMembershipWithDetailsFilter setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public VDepartmentMembershipWithDetailsFilter setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public VDepartmentMembershipWithDetailsFilter setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Boolean getVerified() {
        return verified;
    }

    public VDepartmentMembershipWithDetailsFilter setVerified(Boolean verified) {
        this.verified = verified;
        return this;
    }

    public Boolean getGlobalAdmin() {
        return globalAdmin;
    }

    public VDepartmentMembershipWithDetailsFilter setGlobalAdmin(Boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
        return this;
    }

    public Boolean getDeletedInIdp() {
        return deletedInIdp;
    }

    public VDepartmentMembershipWithDetailsFilter setDeletedInIdp(Boolean deletedInIdp) {
        this.deletedInIdp = deletedInIdp;
        return this;
    }
}
