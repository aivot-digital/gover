package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.prosuna.backend.process.enums.ProcessTaskStatus;
import de.aivot.prosuna.backend.process.projections.DashboardTaskCountsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProcessInstanceTaskRepository extends JpaRepository<ProcessInstanceTaskEntity, Long>, JpaSpecificationExecutor<ProcessInstanceTaskEntity> {
    Optional<ProcessInstanceTaskEntity> findFirstByProcessInstanceIdOrderByStartedDescIdDesc(Long processInstanceId);

    Optional<ProcessInstanceTaskEntity> findByProcessInstanceIdAndAccessKey(Long processInstanceId, String accessKey);

    Optional<ProcessInstanceTaskEntity> findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(Long processInstanceId, Integer processDefinitionNodeId);

    Optional<ProcessInstanceTaskEntity> findFirstByProcessInstanceIdAndProcessNodeIdAndIdNotOrderByStartedDesc(Long processInstanceId,
                                                                                                               Integer processDefinitionNodeId,
                                                                                                               Long excludedTaskId);

    List<ProcessInstanceTaskEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceTaskEntity> findAllByAssignedUserIdInAndStatusIn(Collection<String> assignedUserIds,
                                                                         Collection<ProcessTaskStatus> statuses);

    long countByAssignedUserIdAndStatusIn(String assignedUserId,
                                          Collection<ProcessTaskStatus> statuses);

    @Query(value = """
            SELECT task.*
            FROM process_instance_tasks task
            WHERE task.assigned_user_id = :userId
              AND task.status = :status
              AND (
                    :hasSystemAccess = true
                    OR EXISTS (
                        SELECT 1
                        FROM v_user_process_instance_access_permissions access
                        WHERE access.user_id = :userId
                          AND access.target_process_instance_id = task.process_instance_id
                          AND access.permissions::text[] @> ARRAY[:permission]
                    )
              )
            -- Deadlines take precedence; otherwise tasks waiting the longest are shown first.
            ORDER BY task.deadline ASC NULLS LAST, task.started ASC, task.id ASC
            """, nativeQuery = true)
    List<ProcessInstanceTaskEntity> findDashboardTasks(@Param("userId") String userId,
                                                       @Param("status") short status,
                                                       @Param("hasSystemAccess") boolean hasSystemAccess,
                                                       @Param("permission") String permission,
                                                       Pageable pageable);

    @Query(value = """
            SELECT COUNT(*) AS "totalCount",
                   COUNT(*) FILTER (WHERE task.deadline IS NOT NULL AND task.deadline < :now) AS "overdueCount"
            FROM process_instance_tasks task
            WHERE task.assigned_user_id = :userId
              AND task.status = :status
              AND (
                    :hasSystemAccess = true
                    OR EXISTS (
                        SELECT 1
                        FROM v_user_process_instance_access_permissions access
                        WHERE access.user_id = :userId
                          AND access.target_process_instance_id = task.process_instance_id
                          AND access.permissions::text[] @> ARRAY[:permission]
                    )
              )
            """, nativeQuery = true)
    DashboardTaskCountsProjection getDashboardTaskCounts(@Param("userId") String userId,
                                                          @Param("status") short status,
                                                          @Param("hasSystemAccess") boolean hasSystemAccess,
                                                          @Param("permission") String permission,
                                                          @Param("now") Instant now);

    @Query(
            value = """
                    SELECT distinct on (process_node_id) * from process_instance_tasks where process_instance_id = :processInstanceId order by process_node_id, started desc;
                    """,
            nativeQuery = true
    )
    List<ProcessInstanceTaskEntity> getLatestTasksByProcessInstanceId(@Param("processInstanceId") Long processInstanceId);

    long countAllByStatusIs(ProcessTaskStatus status);
}
