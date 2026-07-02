package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessVersionEntity;
import de.aivot.gover.backend.process.entities.ProcessVersionEntityId;
import de.aivot.gover.backend.process.enums.ProcessVersionStatus;
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

    boolean existsByProcessIdAndStatus(Integer processId, ProcessVersionStatus status);

    @Query(value = """
            SELECT count(*) from process_versions pv
                        JOIN process_nodes pn on pv.process_id = pn.process_id and pv.process_version = pn.process_version
                        WHERE pv.status = :processVersionStatus AND pn.data_key = :s;
            """, nativeQuery = true)
    long countAllByStatusIsAndHasNode(ProcessVersionStatus processVersionStatus, String s);
}