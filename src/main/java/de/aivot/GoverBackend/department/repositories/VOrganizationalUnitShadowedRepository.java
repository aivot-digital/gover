package de.aivot.GoverBackend.department.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.department.entities.VOrganizationalUnitShadowedEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;


public interface VOrganizationalUnitShadowedRepository extends ReadOnlyRepository<VOrganizationalUnitShadowedEntity, Integer>, JpaSpecificationExecutor<VOrganizationalUnitShadowedEntity> {
}
