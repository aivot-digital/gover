package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcessVersionRepository extends JpaRepository<ProcessVersionEntity, ProcessVersionEntityId>, JpaSpecificationExecutor<ProcessVersionEntity> {
    @Query(value = """
            SELECT max(process_definition_version) from process_definition_versions where process_definition_id = :processDefinitionId;
            """, nativeQuery = true)
    Optional<Integer> maxVersionForProcessDefinition(@Param("processDefinitionId") Integer processDefinitionId);
}