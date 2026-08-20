package de.aivot.prosuna.backend.department.repositories;

import de.aivot.prosuna.backend.department.entities.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface DepartmentRepository extends JpaRepository<DepartmentEntity, Integer>, JpaSpecificationExecutor<DepartmentEntity> {
}
