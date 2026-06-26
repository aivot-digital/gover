package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessTestClaimEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProcessTestClaimRepository extends JpaRepository<ProcessTestClaimEntity, Integer>, JpaSpecificationExecutor<ProcessTestClaimEntity> {
    Optional<ProcessTestClaimEntity> findByProcessIdAndAccessKey(@Nonnull Integer processId, @Nonnull String accessKey);

    Optional<ProcessTestClaimEntity> findByProcessIdAndProcessVersion(Integer processId, Integer processVersion);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_process_access_permissions p WHERE p.user_id = :userId AND p.target_process_id = :processId AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasProcessPermission(@Param("userId") String userId,
                                 @Param("processId") Integer processId,
                                 @Param("permission") String permission);

    @Query(
            value = "SELECT DISTINCT p.target_process_id FROM v_user_process_access_permissions p WHERE p.user_id = :userId AND p.permissions::text[] @> ARRAY[:permission]",
            nativeQuery = true
    )
    List<Integer> getProcessIdsWithPermission(@Param("userId") String userId,
                                              @Param("permission") String permission);
}
