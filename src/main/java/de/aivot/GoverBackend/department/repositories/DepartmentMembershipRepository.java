package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.department.entities.DepartmentMembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DepartmentMembershipRepository extends JpaRepository<DepartmentMembershipEntity, Integer>, JpaSpecificationExecutor<DepartmentMembershipEntity> {

}
