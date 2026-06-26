package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.department.entities.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer>, JpaSpecificationExecutor<DepartmentEntity> {
}
