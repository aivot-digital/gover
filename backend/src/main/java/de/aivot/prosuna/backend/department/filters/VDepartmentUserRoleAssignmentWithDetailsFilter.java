package de.aivot.prosuna.backend.department.filters;

import de.aivot.prosuna.backend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import de.aivot.prosuna.backend.lib.models.Filter;
import de.aivot.prosuna.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * @deprecated
 */
@Deprecated
public class VDepartmentUserRoleAssignmentWithDetailsFilter implements Filter<VDepartmentUserRoleAssignmentWithDetailsEntity> {
    private Integer id;
    private Integer departmentId;
    private List<Integer> departmentIds;
    private String name;
    private String userId;
    private String fullName;
    private Integer userRoleId;

    public static VDepartmentUserRoleAssignmentWithDetailsFilter create() {
        return new VDepartmentUserRoleAssignmentWithDetailsFilter();
    }

    @Override
    public Specification<VDepartmentUserRoleAssignmentWithDetailsEntity> build() {
        return SpecificationBuilder
                .create(VDepartmentUserRoleAssignmentWithDetailsEntity.class)
                .withEquals("id", id)
                .withEquals("departmentId", departmentId)
                .withInList("departmentId", departmentIds)
                .withEquals("userId", userId)
                .withContains("fullName", fullName)
                .withContains("name", name)
                .withEquals("userRoleId", userRoleId)
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

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
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

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public VDepartmentUserRoleAssignmentWithDetailsFilter setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
        return this;
    }
}
