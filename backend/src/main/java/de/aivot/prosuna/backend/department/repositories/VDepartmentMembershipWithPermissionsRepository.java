package de.aivot.prosuna.backend.department.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.department.entities.VDepartmentMembershipWithPermissionsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentMembershipWithPermissionsRepository extends ReadOnlyRepository<VDepartmentMembershipWithPermissionsEntity, Integer>, JpaSpecificationExecutor<VDepartmentMembershipWithPermissionsEntity> {
}
