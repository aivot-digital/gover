package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessEntity;
import jakarta.annotation.Nonnull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessRepository extends JpaRepository<ProcessEntity, Integer>, JpaSpecificationExecutor<ProcessEntity> {
    @Query(
            value = "SELECT DISTINCT p.target_process_id FROM v_user_process_access_permissions p WHERE p.user_id = :userId AND p.permissions::text[] @> ARRAY[:permission]",
            nativeQuery = true
    )
    List<Integer> getProcessIdsWithPermission(@Nonnull @Param("userId") String userId,
                                              @Nonnull @Param("permission") String permission);
}
