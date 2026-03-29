package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessVersionEntity;
import de.aivot.GoverBackend.process.entities.ProcessVersionEntityId;
import de.aivot.GoverBackend.process.enums.ProcessVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcessVersionRepository extends JpaRepository<ProcessVersionEntity, ProcessVersionEntityId>, JpaSpecificationExecutor<ProcessVersionEntity> {
    @Query(value = """
            SELECT max(process_version) from process_versions where process_id = :processDefinitionId;
            """, nativeQuery = true)
    Optional<Integer> maxVersionForProcessDefinition(@Param("processDefinitionId") Integer processDefinitionId);

    long countAllByStatusIs(ProcessVersionStatus status);
}