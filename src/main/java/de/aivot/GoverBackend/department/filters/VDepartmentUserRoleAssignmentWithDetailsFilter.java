package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;


public class VDepartmentUserRoleAssignmentWithDetailsFilter implements Filter<VDepartmentUserRoleAssignmentWithDetailsEntity> {
    private Integer id;
    private Integer departmentId;
    private String name;
    private String userId;
    private String fullName;

    public static VDepartmentUserRoleAssignmentWithDetailsFilter create() {
        return new VDepartmentUserRoleAssignmentWithDetailsFilter();
    }

    @Override
    public Specification<VDepartmentUserRoleAssignmentWithDetailsEntity> build() {
        return SpecificationBuilder
                .create(VDepartmentUserRoleAssignmentWithDetailsEntity.class)
                .withEquals("id", id)
                .withEquals("departmentId", departmentId)
                .withEquals("userId", userId)
                .withContains("fullName", fullName)
                .withContains("name", name)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getFullName() {
        return fullName;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }

    public String getName() {
        return name;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setName(String name) {
        this.name = name;
        return this;
    }
}
