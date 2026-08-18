package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessInstanceEntity;
import de.aivot.gover.backend.process.enums.ProcessInstanceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEntity> {
    List<ProcessInstanceEntity> findAllByStatus(ProcessInstanceStatus status);

    Optional<ProcessInstanceEntity> findByAccessKey(String accessKey);

    List<ProcessInstanceEntity> findAllByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                                         Instant keepUntil,
                                                                         Pageable pageable);

    long countByStatusAndKeepUntilLessThanEqual(ProcessInstanceStatus status,
                                                Instant keepUntil);

    List<ProcessInstanceEntity> findAllByCreatedForTestClaimId(Integer createdForTestClaimId);

    long countAllByStatusIs(ProcessInstanceStatus status);

    boolean existsByCaseNumber(String caseNumber);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_process_instance_access_permissions p WHERE p.user_id = :userId AND p.target_process_instance_id = :processInstanceId AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermission(@Param("userId") String userId,
                          @Param("processInstanceId") Long processInstanceId,
                          @Param("permission") String permission);

    @Query(
            value = "SELECT DISTINCT p.target_process_instance_id FROM v_user_process_instance_access_permissions p WHERE p.user_id = :userId AND p.target_process_instance_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission]",
            nativeQuery = true
    )
    List<Long> getProcessInstanceIdsWithPermission(@Param("userId") String userId,
                                                   @Param("permission") String permission);

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM v_user_process_instance_access_permissions p WHERE p.user_id = :userId AND p.target_process_instance_id IS NOT NULL AND p.permissions::text[] @> ARRAY[:permission])",
            nativeQuery = true
    )
    boolean hasPermissionInAnyProcessInstance(@Param("userId") String userId,
                                              @Param("permission") String permission);

    /**
     * Reads the highest increment used for the already rendered static parts of a case number template.
     *
     * <p>The query deliberately compares prefix/suffix and the overall character length instead of using a shared ID
     * generator helper. That keeps the process module independent and lets the increment reset automatically whenever
     * one of the temporal placeholders changes.</p>
     */
    @Query(value = """
            SELECT MAX(CAST(SUBSTRING(case_number FROM :incrementStart FOR :padding) AS INTEGER))
            FROM process_instances
            WHERE CHAR_LENGTH(case_number) = :expectedLength
              AND LEFT(case_number, :prefixLength) = :prefix
              AND RIGHT(case_number, :suffixLength) = :suffix
              AND SUBSTRING(case_number FROM :incrementStart FOR :padding) ~ '^[0-9]+$'
            """, nativeQuery = true)
    Integer getMaxCaseNumberIncrement(@Param("prefix") String prefix,
                                      @Param("suffix") String suffix,
                                      @Param("prefixLength") int prefixLength,
                                      @Param("suffixLength") int suffixLength,
                                      @Param("incrementStart") int incrementStart,
                                      @Param("padding") int padding,
                                      @Param("expectedLength") int expectedLength);
}
