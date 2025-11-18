package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.department.entities.DepartmentEntity;
import de.aivot.GoverBackend.department.entities.ShadowedOrganizationalUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface ShadowedOrganizationalUnitRepository extends JpaRepository<ShadowedOrganizationalUnitEntity, Integer>, JpaSpecificationExecutor<ShadowedOrganizationalUnitEntity> {
}
