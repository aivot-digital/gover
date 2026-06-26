package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @deprecated
 */
@Deprecated
public interface VDepartmentUserRoleAssignmentWithDetailsRepository extends ReadOnlyRepository<VDepartmentUserRoleAssignmentWithDetailsEntity, Integer>, JpaSpecificationExecutor<VDepartmentUserRoleAssignmentWithDetailsEntity> {
}
