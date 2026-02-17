package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProcessEdgeRepository extends JpaRepository<ProcessEdgeEntity, Integer>, JpaSpecificationExecutor<ProcessEdgeEntity> {
    Optional<ProcessEdgeEntity> findByFromNodeIdAndViaPort(Integer fromNodeId, String viaPort);
}