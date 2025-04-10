package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntity;
import de.aivot.GoverBackend.department.entities.DepartmentWithMembershipEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface DepartmentWithMembershipRepository extends JpaRepository<DepartmentWithMembershipEntity, DepartmentWithMembershipEntityId>, JpaSpecificationExecutor<DepartmentWithMembershipEntity> {

}
