package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessInstanceTaskRepository extends JpaRepository<ProcessInstanceTaskEntity, Long>, JpaSpecificationExecutor<ProcessInstanceTaskEntity> {
    ProcessInstanceTaskEntity findFirstByProcessInstanceIdAndProcessNodeIdOrderByStartedDesc(Long processInstanceId, Integer processDefinitionNodeId);

    @Query(
            value = """
                    SELECT distinct on (process_node_id) * from process_instance_tasks where process_instance_id = :processInstanceId order by process_node_id, started desc;
                    """,
            nativeQuery = true
    )
    List<ProcessInstanceTaskEntity> getLatestTasksByProcessInstanceId(@Param("processInstanceId") Long processInstanceId);
}