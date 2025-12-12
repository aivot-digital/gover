package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProcessDefinitionEdgeRepository extends JpaRepository<ProcessDefinitionEdgeEntity, Integer>, JpaSpecificationExecutor<ProcessDefinitionEdgeEntity> {
    Optional<ProcessDefinitionEdgeEntity> findByFromNodeIdAndViaPort(Integer fromNodeId, String viaPort);
}