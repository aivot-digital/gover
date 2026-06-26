package de.aivot.gover.backend.department.repositories;

import de.aivot.gover.backend.core.repositories.ReadOnlyRepository;
import de.aivot.gover.backend.department.entities.VDepartmentShadowedEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VDepartmentShadowedRepository extends ReadOnlyRepository<VDepartmentShadowedEntity, Integer>, JpaSpecificationExecutor<VDepartmentShadowedEntity> {
}
