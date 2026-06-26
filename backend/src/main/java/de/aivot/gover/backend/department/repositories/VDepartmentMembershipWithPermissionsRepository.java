package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.department.entities.VDepartmentMembershipWithPermissionsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentMembershipWithPermissionsRepository extends ReadOnlyRepository<VDepartmentMembershipWithPermissionsEntity, Integer>, JpaSpecificationExecutor<VDepartmentMembershipWithPermissionsEntity> {
}
