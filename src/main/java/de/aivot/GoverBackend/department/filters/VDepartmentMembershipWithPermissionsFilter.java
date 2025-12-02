package de.aivot.GoverBackend.department.filters;

import de.aivot.GoverBackend.department.entities.VDepartmentMembershipWithPermissionsEntity;
import de.aivot.GoverBackend.lib.models.Filter;
import de.aivot.GoverBackend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;


public class VDepartmentMembershipWithPermissionsFilter implements Filter<VDepartmentMembershipWithPermissionsEntity> {
    private Integer id;
    private Integer departmentId;
    private String userId;
    private Boolean departmentPermissionEdit;
    private Boolean formPermissionCreate;
    private Boolean formPermissionEdit;

    public static VDepartmentMembershipWithPermissionsFilter create() {
        return new VDepartmentMembershipWithPermissionsFilter();
    }

    @Override
    public Specification<VDepartmentMembershipWithPermissionsEntity> build() {
        return SpecificationBuilder
                .create(VDepartmentMembershipWithPermissionsEntity.class)
                .withEquals("id", id)
                .withEquals("departmentId", departmentId)
                .withEquals("userId", userId)
                .withEquals("departmentPermissionEdit", departmentPermissionEdit)
                .withEquals("formPermissionCreate", formPermissionCreate)
                .withEquals("formPermissionEdit", formPermissionEdit)
                .build();
    }

    public Integer getId() {
        return id;
    }

    public VDepartmentMembershipWithPermissionsFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public VDepartmentMembershipWithPermissionsFilter setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public VDepartmentMembershipWithPermissionsFilter setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public Boolean getDepartmentPermissionEdit() {
        return departmentPermissionEdit;
    }

    public VDepartmentMembershipWithPermissionsFilter setDepartmentPermissionEdit(Boolean departmentPermissionEdit) {
        this.departmentPermissionEdit = departmentPermissionEdit;
        return this;
    }

    public VDepartmentMembershipWithPermissionsFilter setFormPermissionCreate(Boolean formPermissionCreate) {
        this.formPermissionCreate = formPermissionCreate;
        return this;
    }

    public VDepartmentMembershipWithPermissionsFilter setFormPermissionEdit(Boolean formPermissionEdit) {
        this.formPermissionEdit = formPermissionEdit;
        return this;
    }
}
