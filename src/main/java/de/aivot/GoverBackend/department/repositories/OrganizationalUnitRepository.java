package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.department.entities.OrganizationalUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnitEntity, Integer>, JpaSpecificationExecutor<OrganizationalUnitEntity> {
}
