package de.aivot.gover.backend.permissions.repositories;

import de.aivot.gover.backend.permissions.entities.VUserDomainPermissionEntity;
import de.aivot.gover.backend.permissions.entities.VUserDomainPermissionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VUserDomainPermissionRepository extends JpaRepository<VUserDomainPermissionEntity, VUserDomainPermissionEntityId>, JpaSpecificationExecutor<VUserDomainPermissionEntity> {
    List<VUserDomainPermissionEntity> findAllByUserId(String userId);
}
