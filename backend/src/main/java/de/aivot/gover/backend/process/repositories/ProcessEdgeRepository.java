package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ProcessEdgeRepository extends JpaRepository<ProcessEdgeEntity, Integer>, JpaSpecificationExecutor<ProcessEdgeEntity> {
    List<ProcessEdgeEntity> findAllByProcessIdAndProcessVersion(Integer processId, Integer processVersion);
    Optional<ProcessEdgeEntity> findByFromNodeIdAndViaPort(Integer fromNodeId, String viaPort);
    boolean existsByFromNodeIdAndViaPort(Integer fromNodeId, String viaPort);
}
