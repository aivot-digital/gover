package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessRepository extends JpaRepository<ProcessEntity, Integer>, JpaSpecificationExecutor<ProcessEntity> {
    @Query(
            value = "SELECT DISTINCT p.target_process_id FROM v_user_process_access_permissions p WHERE p.user_id = :userId AND p.target_process_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission]",
            nativeQuery = true
    )
    List<Integer> getProcessIdsWithPermission(@Nonnull @Param("userId") String userId,
                                              @Nonnull @Param("permission") String permission);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_process_access_permissions p WHERE p.user_id = :userId AND p.target_process_id = :processId AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermission(@Nonnull @Param("userId") String userId,
                          @Nonnull @Param("processId") Integer processId,
                          @Nonnull @Param("permission") String permission);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_process_access_permissions p WHERE p.user_id = :userId AND p.target_process_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermissionInAnyProcess(@Nonnull @Param("userId") String userId,
                                      @Nonnull @Param("permission") String permission);

    Optional<ProcessEntity> findByAccessKey(UUID accessKey);

    Optional<ProcessEntity> findBySlug(@Nonnull String slug);

    boolean existsBySlug(@Nonnull String slug);

    boolean existsBySlugAndIdIsNot(@Nonnull String slug, @Nonnull Integer id);
}
