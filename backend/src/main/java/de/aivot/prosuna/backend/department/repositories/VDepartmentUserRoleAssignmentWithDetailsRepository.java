package de.aivot.prosuna.backend.department.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @deprecated
 */
@Deprecated
public interface VDepartmentUserRoleAssignmentWithDetailsRepository extends ReadOnlyRepository<VDepartmentUserRoleAssignmentWithDetailsEntity, Integer>, JpaSpecificationExecutor<VDepartmentUserRoleAssignmentWithDetailsEntity> {
}
