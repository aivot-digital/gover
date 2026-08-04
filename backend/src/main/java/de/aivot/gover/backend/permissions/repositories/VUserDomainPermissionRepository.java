package de.aivot.gover.backend.permissions.repositories;

import de.aivot.gover.backend.permissions.entities.VUserDomainPermissionEntity;
import de.aivot.gover.backend.permissions.entities.VUserDomainPermissionEntityId;
import de.aivot.gover.backend.permissions.projections.DomainPermissionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VUserDomainPermissionRepository extends JpaRepository<VUserDomainPermissionEntity, VUserDomainPermissionEntityId>, JpaSpecificationExecutor<VUserDomainPermissionEntity> {
    List<VUserDomainPermissionEntity> findAllByUserId(String userId);

    @Query("""
            SELECT permission.userId AS userId,
                   permission.departmentId AS departmentId,
                   permission.teamId AS teamId,
                   permission.permissions AS permissions
            FROM VUserDomainPermissionEntity permission
            WHERE permission.userId = :userId
              AND (permission.departmentId IS NOT NULL OR permission.teamId IS NOT NULL)
              AND array_length(permission.permissions) > 0
            """)
    List<DomainPermissionProjection> findAllConcreteByUserId(@Param("userId") String userId);
}
