package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProcessDefinitionNodeRepository extends JpaRepository<ProcessDefinitionNodeEntity, Integer>, JpaSpecificationExecutor<ProcessDefinitionNodeEntity> {
    List<ProcessDefinitionNodeEntity> findAllByProcessDefinitionIdAndProcessDefinitionVersion(Integer processDefinitionId, Integer processDefinitionVersion);
}