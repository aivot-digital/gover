package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.department.entities.VDepartmentUserRoleAssignmentWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @deprecated
 */
@Deprecated
public interface VDepartmentUserRoleAssignmentWithDetailsRepository extends ReadOnlyRepository<VDepartmentUserRoleAssignmentWithDetailsEntity, Integer>, JpaSpecificationExecutor<VDepartmentUserRoleAssignmentWithDetailsEntity> {
}
