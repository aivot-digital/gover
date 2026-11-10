package de.aivot.prosuna.backend.permissions.repositories;

import de.aivot.prosuna.backend.permissions.entities.VUserSystemPermissionEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VUserSystemPermissionRepository extends JpaRepository<VUserSystemPermissionEntity, String>, JpaSpecificationExecutor<VUserSystemPermissionEntity> {
    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_system_permission p WHERE p.user_id = :userId AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermission(@Param("userId") String userId,
                          @Param("permission") String permission);

    // Same grants as hasPermission for several users at once. "= ANY" matches like "@>" and also runs on H2 in tests.
    @Query(value = """
            SELECT p.user_id
            FROM v_user_system_permission p
            WHERE p.user_id IN (:userIds)
              AND :permission = ANY(p.permissions)
            """, nativeQuery = true)
    List<String> findUserIdsWithPermission(@Nonnull @Param("userIds") Collection<String> userIds,
                                           @Nonnull @Param("permission") String permission);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1
                FROM users usr
                JOIN system_roles role ON role.id = usr.system_role_id
                WHERE usr.id = :userId
                  AND role.permissions::text[] @> ARRAY[:permission]
            )
            """, nativeQuery = true)
    boolean hasPermissionWithoutDeputies(@Nonnull @Param("userId") String userId,
                                         @Nonnull @Param("permission") String permission);

    List<VUserSystemPermissionEntity> findAllByUserId(String userId);
}
