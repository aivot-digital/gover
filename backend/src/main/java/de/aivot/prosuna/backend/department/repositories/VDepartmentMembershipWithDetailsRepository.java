package de.aivot.prosuna.backend.department.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.department.entities.VDepartmentMembershipWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentMembershipWithDetailsRepository extends ReadOnlyRepository<VDepartmentMembershipWithDetailsEntity, String>, JpaSpecificationExecutor<VDepartmentMembershipWithDetailsEntity> {
}
