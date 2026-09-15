package de.aivot.prosuna.backend.permissions.repositories;

import de.aivot.prosuna.backend.permissions.entities.VUserTeamPermissionEntity;
import de.aivot.prosuna.backend.permissions.entities.VUserTeamPermissionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VUserTeamPermissionRepository extends JpaRepository<VUserTeamPermissionEntity, VUserTeamPermissionEntityId>, JpaSpecificationExecutor<VUserTeamPermissionEntity> {
    @Query(
            value = "SELECT * FROM v_user_team_permissions p WHERE p.user_id = :userId AND p.team_id IS NOT NULL AND array_length(p.permissions, 1) > 0",
            nativeQuery = true
    )
    List<VUserTeamPermissionEntity> findAllByUserId(@Param("userId") String userId);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_team_permissions p WHERE p.user_id = :userId AND p.team_id = :teamId AND p.team_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermission(@Param("userId") String userId,
                          @Param("teamId") Integer teamId,
                          @Param("permission") String permission);

    // The permission view also contains system-derived rows without a concrete team.
    // Resource-scoped list filters must only receive real team IDs.
    @Query(
            value = "SELECT p.team_id FROM v_user_team_permissions p WHERE p.user_id = :userId AND p.team_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission]",
            nativeQuery = true
    )
    List<Integer> getTeamsWithPermission(@Param("userId") String userId,
                                         @Param("permission") String permission);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_team_permissions p WHERE p.user_id = :userId AND p.team_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermissionInAnyTeam(@Param("userId") String userId,
                                   @Param("permission") String permission);
}
