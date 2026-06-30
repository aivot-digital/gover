package de.aivot.gover.backend.permissions.repositories;

import de.aivot.gover.backend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.gover.backend.permissions.entities.VUserTeamPermissionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VUserTeamPermissionRepository extends JpaRepository<VUserTeamPermissionEntity, VUserTeamPermissionEntityId>, JpaSpecificationExecutor<VUserTeamPermissionEntity> {
    List<VUserTeamPermissionEntity> findAllByUserId(String userId);
}
