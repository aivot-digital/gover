package de.aivot.GoverBackend.permissions.repositories;

import de.aivot.GoverBackend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.GoverBackend.permissions.entities.VUserTeamPermissionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VUserTeamPermissionRepository extends JpaRepository<VUserTeamPermissionEntity, VUserTeamPermissionEntityId>, JpaSpecificationExecutor<VUserTeamPermissionEntity> {
}
