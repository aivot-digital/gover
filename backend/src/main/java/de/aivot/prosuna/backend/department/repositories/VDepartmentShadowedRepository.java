package de.aivot.prosuna.backend.department.repositories;

import de.aivot.prosuna.backend.core.repositories.ReadOnlyRepository;
import de.aivot.prosuna.backend.department.entities.VDepartmentShadowedEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentShadowedRepository extends ReadOnlyRepository<VDepartmentShadowedEntity, Integer>, JpaSpecificationExecutor<VDepartmentShadowedEntity> {
}
