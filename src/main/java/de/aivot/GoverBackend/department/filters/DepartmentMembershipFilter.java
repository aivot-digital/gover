package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;


public class DepartmentMembershipFilter implements Filter<DepartmentMembershipEntity> {
    private Integer departmentId;
    private String userId;
    private List<String> userIds;
    private List<Integer> departmentIds;
    private UserRole role;
    private String name;
    private String departmentName;

    public static DepartmentMembershipFilter create() {
        return new DepartmentMembershipFilter();
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public DepartmentMembershipFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public DepartmentMembershipFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public UserRole getRole() {
        return role;
    }

    public DepartmentMembershipFilter setRole(UserRole role) {
        this.role = role;
        return this;
    }

    public String getName() {
        return name;
    }

    public DepartmentMembershipFilter setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public DepartmentMembershipFilter setUserIds(List<String> userIds) {
        this.userIds = userIds;
        return this;
    }

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public DepartmentMembershipFilter setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
        return this;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    @Override
    public Specification<DepartmentMembershipEntity> build() {
        return SpecificationBuilder
                .create(DepartmentMembershipEntity.class)
                .withEquals("departmentId", departmentId)
                .withEquals("userId", userId)
                .withInList("userId", userIds)
                .withInList("departmentId", departmentIds)
                .withEquals("role", role)
                .build();
    }
}
