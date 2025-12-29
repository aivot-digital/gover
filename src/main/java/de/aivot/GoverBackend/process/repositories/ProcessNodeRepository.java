package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProcessNodeRepository extends JpaRepository<ProcessNodeEntity, Integer>, JpaSpecificationExecutor<ProcessNodeEntity> {
    List<ProcessNodeEntity> findAllByProcessIdAndProcessVersion(Integer processDefinitionId, Integer processDefinitionVersion);
}