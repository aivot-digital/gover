package de.aivot.GoverBackend.userRoles.repositories;

import de.aivot.GoverBackend.core.repositories.ReadOnlyRepository;
import de.aivot.GoverBackend.userRoles.entities.VUserDomainPermissionEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VUserDomainPermissionRepository extends ReadOnlyRepository<VUserDomainPermissionEntity, String>, JpaSpecificationExecutor<VUserDomainPermissionEntity> {
    @Query(
            value = """
                    SELECT exists(select 1 from v_user_domain_permission where user_id = :userId and department_id = :departmentId and :permission = any(permissions));
                    """,
            nativeQuery = true
    )
    boolean existsByUserIdIsAndDepartmentIdAndPermission(@Param("userId") String userId,
                                                         @Param("departmentId") Integer departmentId,
                                                         @Param("permission") String permission);

    @Query(
            value = """
                    SELECT exists(select 1 from v_user_domain_permission where user_id = :userId and team_id = :teamId and :permission = any(permissions));
                    """,
            nativeQuery = true
    )
    boolean existsByUserIdIsAndTeamIdAndPermission(@Param("userId") String userId,
                                                   @Param("teamId") Integer teamId,
                                                   @Param("permission") String permission);
}