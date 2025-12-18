package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.userRoles.entities.VUserSystemPermissionEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VUserSystemPermissionRepository extends ReadOnlyRepository<VUserSystemPermissionEntity, String>, JpaSpecificationExecutor<VUserSystemPermissionEntity> {
    @Query(
            value = """
                    SELECT exists(select 1 from v_user_domain_permission where user_id = :userId and :permission = any(permissions));
                    """,
            nativeQuery = true
    )
    boolean existsByUserIdIsAndPermission(@Param("userId") String userId, @Param("permission") String permission);
}

