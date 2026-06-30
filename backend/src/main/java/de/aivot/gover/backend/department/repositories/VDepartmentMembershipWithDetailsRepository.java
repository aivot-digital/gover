package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.department.entities.VDepartmentMembershipWithDetailsEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentMembershipWithDetailsRepository extends ReadOnlyRepository<VDepartmentMembershipWithDetailsEntity, String>, JpaSpecificationExecutor<VDepartmentMembershipWithDetailsEntity> {
}
