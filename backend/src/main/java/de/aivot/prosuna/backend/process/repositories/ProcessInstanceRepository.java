package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEntity;
import de.aivot.prosuna.backend.process.enums.ProcessInstanceStatus;
import de.aivot.prosuna.backend.process.projections.DashboardActivityBucketProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface ProcessInstanceRepository extends JpaRepository<ProcessInstanceEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEntity> {
    List<ProcessInstanceEntity> findAllByStatus(ProcessInstanceStatus status);

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

    @Query(value = """
            WITH periods AS (
                SELECT generate_series(
                    CAST(:firstPeriodStart AS date),
                    CAST(:lastPeriodStart AS date),
                    make_interval(days => :bucketDays)
                ) AS period_start
            ),
            accessible_instances AS MATERIALIZED (
                -- Keep the materialized access set narrow; instance payloads can be large and are irrelevant here.
                SELECT instance.id, instance.status, instance.started, instance.finished
                FROM process_instances instance
                WHERE instance.created_for_test_claim_id IS NULL
                  AND (instance.started >= :firstPeriodStart OR instance.finished >= :firstPeriodStart)
                  AND (
                        :hasSystemAccess = true
                        OR EXISTS (
                            SELECT 1
                            FROM v_user_process_instance_access_permissions access
                            WHERE access.user_id = :userId
                              AND access.target_process_instance_id = instance.id
                              AND access.permissions::text[] @> ARRAY[:permission]
                        )
                  )
            )
            SELECT periods.period_start::date AS "periodStart",
                   COUNT(DISTINCT instance.id) FILTER (
                       WHERE instance.started >= periods.period_start
                         AND instance.started < periods.period_start + make_interval(days => :bucketDays)
                   ) AS "startedCount",
                   COUNT(DISTINCT instance.id) FILTER (
                       WHERE instance.status = :completedStatus
                         AND instance.finished >= periods.period_start
                         AND instance.finished < periods.period_start + make_interval(days => :bucketDays)
                   ) AS "completedCount"
            FROM periods
            LEFT JOIN accessible_instances instance
              ON (instance.started >= periods.period_start AND instance.started < periods.period_start + make_interval(days => :bucketDays))
              OR (instance.finished >= periods.period_start AND instance.finished < periods.period_start + make_interval(days => :bucketDays))
            GROUP BY periods.period_start
            ORDER BY periods.period_start
            """, nativeQuery = true)
    List<DashboardActivityBucketProjection> getDashboardActivity(@Param("userId") String userId,
                                                                 @Param("hasSystemAccess") boolean hasSystemAccess,
                                                                 @Param("permission") String permission,
                                                                 @Param("completedStatus") int completedStatus,
                                                                 @Param("firstPeriodStart") LocalDate firstPeriodStart,
                                                                 @Param("lastPeriodStart") LocalDate lastPeriodStart,
                                                                 @Param("bucketDays") int bucketDays);

    @Query(value = """
            SELECT COUNT(*)
            FROM process_instances instance
            WHERE instance.created_for_test_claim_id IS NULL
              AND instance.status = :runningStatus
              AND (
                    :hasSystemAccess = true
                    OR EXISTS (
                        SELECT 1
                        FROM v_user_process_instance_access_permissions access
                        WHERE access.user_id = :userId
                          AND access.target_process_instance_id = instance.id
                          AND access.permissions::text[] @> ARRAY[:permission]
                    )
              )
            """, nativeQuery = true)
    long countActiveDashboardInstances(@Param("userId") String userId,
                                       @Param("hasSystemAccess") boolean hasSystemAccess,
                                       @Param("permission") String permission,
                                       @Param("runningStatus") int runningStatus);

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
