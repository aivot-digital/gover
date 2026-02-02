package de.aivot.GoverBackend.permissions.repositories;

import de.aivot.GoverBackend.permissions.entities.VUserDomainPermissionEntity;
import de.aivot.GoverBackend.permissions.entities.VUserDomainPermissionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VUserDomainPermissionRepository extends JpaRepository<VUserDomainPermissionEntity, VUserDomainPermissionEntityId>, JpaSpecificationExecutor<VUserDomainPermissionEntity> {
}
