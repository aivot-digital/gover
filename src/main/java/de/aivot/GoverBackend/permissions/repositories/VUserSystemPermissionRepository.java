package de.aivot.GoverBackend.permissions.repositories;

import de.aivot.GoverBackend.permissions.entities.VUserSystemPermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VUserSystemPermissionRepository extends JpaRepository<VUserSystemPermissionEntity, String>, JpaSpecificationExecutor<VUserSystemPermissionEntity> {
    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_system_permission p WHERE p.user_id = :userId AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermission(@Param("userId") String userId,
                          @Param("permission") String permission);
}
