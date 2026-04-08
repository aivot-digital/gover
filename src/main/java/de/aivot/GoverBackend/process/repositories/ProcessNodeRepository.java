package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ProcessNodeRepository extends JpaRepository<ProcessNodeEntity, Integer>, JpaSpecificationExecutor<ProcessNodeEntity> {
    List<ProcessNodeEntity> findAllByProcessId(Integer processDefinitionId);

    List<ProcessNodeEntity> findAllByProcessIdAndProcessVersion(Integer processId, Integer processVersion);


    @Query(
            value = """
                                        SELECT DISTINCT node.dataKey
                                        FROM ProcessNodeEntity node
                                        WHERE node.processId = :processId
                                        AND node.processVersion = :processVersion
                    """
    )
    Set<String> findAllDataKeysByProcessIdAndVersion(
            @Param("processId") Integer processId,
            @Param("processVersion") Integer processVersion
    );

    boolean existsByDataKeyAndIdIsNotAndProcessIdAndProcessVersion(String dataKey, Integer nodeId, Integer processId, Integer processVersion);
}