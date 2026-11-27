package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessNodeEntity;
import de.aivot.prosuna.backend.process.enums.ProcessVersionStatus;
import de.aivot.prosuna.backend.process.projections.ProcessNodeDefinitionCountProjection;
import de.aivot.prosuna.backend.process.projections.ProcessVersionStatusCountProjection;
import jakarta.annotation.Nonnull;
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

    long countAllByProcessNodeDefinitionKey(String processNodeDefinitionKey);

    @Nonnull
    @Query("""
            select node.processNodeDefinitionKey as definitionKey,
                   node.processNodeDefinitionVersion as definitionVersion,
                   count(node) as total
            from ProcessNodeEntity node
            join ProcessVersionEntity version
              on version.processId = node.processId and version.processVersion = node.processVersion
            where version.status = :status
            group by node.processNodeDefinitionKey, node.processNodeDefinitionVersion
            """)
    List<ProcessNodeDefinitionCountProjection> countByDefinition(@Nonnull @Param("status") ProcessVersionStatus status);

    @Nonnull
    @Query("""
            select version.status as status, count(node) as total
            from ProcessNodeEntity node
            join ProcessVersionEntity version
              on version.processId = node.processId and version.processVersion = node.processVersion
            where node.savedWithErrors = true
            group by version.status
            """)
    List<ProcessVersionStatusCountProjection> countSavedWithErrorsByVersionStatus();
}
