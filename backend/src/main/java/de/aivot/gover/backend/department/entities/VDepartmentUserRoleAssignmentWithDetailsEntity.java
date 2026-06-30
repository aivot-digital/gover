package de.aivot.gover.backend.department.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @deprecated
 */
@Deprecated
@Entity
@Table(name = "v_department_user_role_assignments_with_details")
public class VDepartmentUserRoleAssignmentWithDetailsEntity {
    @Id
    private Integer id;

    public Integer getId() {
        return id;
    }

    public VDepartmentUserRoleAssignmentWithDetailsEntity setId(Integer id) {
        this.id = id;
        return this;
    }
}
