package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.userRoles.entities.LegacyVUserSystemPermissionEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Deprecated
/**
 * @deprecated Use UserSystemPermissionRepository instead
 */
@Repository
public interface LegacyVUserSystemPermissionRepository extends ReadOnlyRepository<LegacyVUserSystemPermissionEntity, String>, JpaSpecificationExecutor<LegacyVUserSystemPermissionEntity> {
    @Query(
            value = """
                    SELECT exists(select 1 from v_user_domain_permission where user_id = :userId and :permission = any(permissions));
                    """,
            nativeQuery = true
    )
    boolean existsByUserIdIsAndPermission(@Param("userId") String userId, @Param("permission") String permission);
}

