package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import de.aivot.GoverBackend.process.enums.ProcessTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProcessInstanceTaskRepository extends JpaRepository<ProcessInstanceTaskEntity, Long>, JpaSpecificationExecutor<ProcessInstanceTaskEntity> {
    ProcessInstanceTaskEntity findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(Long processInstanceId, Integer processDefinitionNodeId);

    List<ProcessInstanceTaskEntity> findAllByProcessInstanceId(Long processInstanceId);

    List<ProcessInstanceTaskEntity> findAllByAssignedUserIdInAndStatusIn(Collection<String> assignedUserIds,
                                                                         Collection<ProcessTaskStatus> statuses);

    @Query(
            value = """
                    SELECT distinct on (process_node_id) * from process_instance_tasks where process_instance_id = :processInstanceId order by process_node_id, started desc;
                    """,
            nativeQuery = true
    )
    List<ProcessInstanceTaskEntity> getLatestTasksByProcessInstanceId(@Param("processInstanceId") Long processInstanceId);
}
