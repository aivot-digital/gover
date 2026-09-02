package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.permissions.projections.ProcessInstancePermissionProjection;
import de.aivot.prosuna.backend.process.entities.VUserProcessInstanceAccessPermissionsEntity;
import de.aivot.prosuna.backend.process.entities.VUserProcessInstanceAccessPermissionsEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VUserProcessInstanceAccessPermissionsRepository extends JpaRepository<VUserProcessInstanceAccessPermissionsEntity, VUserProcessInstanceAccessPermissionsEntityId> {
    @Query("""
            SELECT permission.userId AS userId,
                   permission.viaSourceTeamId AS viaSourceTeamId,
                   permission.viaSourceDepartmentId AS viaSourceDepartmentId,
                   permission.targetProcessInstanceId AS processInstanceId,
                   permission.permissions AS permissions
            FROM VUserProcessInstanceAccessPermissionsEntity permission
            WHERE permission.userId = :userId
              AND permission.targetProcessInstanceId IS NOT NULL
              AND array_length(permission.permissions) > 0
            """)
    List<ProcessInstancePermissionProjection> findAllConcreteByUserId(@Param("userId") String userId);
}
