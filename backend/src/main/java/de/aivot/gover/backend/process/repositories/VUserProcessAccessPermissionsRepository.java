package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.permissions.projections.ProcessPermissionProjection;
import de.aivot.gover.backend.process.entities.VUserProcessAccessPermissionsEntity;
import de.aivot.gover.backend.process.entities.VUserProcessAccessPermissionsEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VUserProcessAccessPermissionsRepository extends JpaRepository<VUserProcessAccessPermissionsEntity, VUserProcessAccessPermissionsEntityId> {
    @Query("""
            SELECT permission.userId AS userId,
                   permission.viaSourceTeamId AS viaSourceTeamId,
                   permission.viaSourceDepartmentId AS viaSourceDepartmentId,
                   permission.targetProcessId AS processId,
                   permission.permissions AS permissions
            FROM VUserProcessAccessPermissionsEntity permission
            WHERE permission.userId = :userId
              AND permission.targetProcessId IS NOT NULL
              AND array_length(permission.permissions) > 0
            """)
    List<ProcessPermissionProjection> findAllConcreteByUserId(@Param("userId") String userId);
}
