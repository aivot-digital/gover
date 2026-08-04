package de.aivot.gover.backend.department.filters;

import de.aivot.gover.backend.department.entities.VDepartmentMembershipWithPermissionsEntity;
import de.aivot.gover.backend.lib.models.Filter;
import de.aivot.gover.backend.utils.specification.SpecificationBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class VDepartmentMembershipWithPermissionsFilter implements Filter<VDepartmentMembershipWithPermissionsEntity> {
    private Integer id;
    private Integer departmentId;
    private List<Integer> departmentIds;
    private String userId;
    private Boolean departmentPermissionEdit;
    private Boolean formPermissionCreate;
    private Boolean formPermissionEdit;
    private Boolean processPermissionCreate;
    private Boolean processPermissionRead;
    private Boolean processPermissionEdit;

    public static VDepartmentMembershipWithPermissionsFilter create() {
        return new VDepartmentMembershipWithPermissionsFilter();
    }

    @Override
    public Specification<VDepartmentMembershipWithPermissionsEntity> build() {
        return SpecificationBuilder
                .create(VDepartmentMembershipWithPermissionsEntity.class)
                .withEquals("id", id)
                .withEquals("departmentId", departmentId)
                .withInList("departmentId", departmentIds)
                .withEquals("userId", userId)
                .withEquals("departmentPermissionEdit", departmentPermissionEdit)
                .withEquals("formPermissionCreate", formPermissionCreate)
                .withEquals("formPermissionEdit", formPermissionEdit)
                .withEquals("processPermissionCreate", processPermissionCreate)
                .withEquals("processPermissionRead", processPermissionRead)
                .withEquals("processPermissionEdit", processPermissionEdit)
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

    public List<Integer> getDepartmentIds() {
        return departmentIds;
    }

    public VDepartmentMembershipWithPermissionsFilter setDepartmentIds(List<Integer> departmentIds) {
        this.departmentIds = departmentIds;
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

    public VDepartmentMembershipWithPermissionsFilter setProcessPermissionCreate(Boolean processPermissionCreate) {
        this.processPermissionCreate = processPermissionCreate;
        return this;
    }

    public VDepartmentMembershipWithPermissionsFilter setProcessPermissionEdit(Boolean processPermissionEdit) {
        this.processPermissionEdit = processPermissionEdit;
        return this;
    }

    public VDepartmentMembershipWithPermissionsFilter setProcessPermissionRead(Boolean processPermissionRead) {
        this.processPermissionRead = processPermissionRead;
        return this;
    }
}
