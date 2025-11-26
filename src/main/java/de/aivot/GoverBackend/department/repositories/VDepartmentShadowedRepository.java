package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.department.entities.VDepartmentShadowedEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentShadowedRepository extends ReadOnlyRepository<VDepartmentShadowedEntity, Integer>, JpaSpecificationExecutor<VDepartmentShadowedEntity> {
}
