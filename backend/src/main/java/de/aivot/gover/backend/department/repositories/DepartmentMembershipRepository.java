package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.department.entities.DepartmentMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembershipEntity, Integer>, JpaSpecificationExecutor<DepartmentMembershipEntity> {

}
