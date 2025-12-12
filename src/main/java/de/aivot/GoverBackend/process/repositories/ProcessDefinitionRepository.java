package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessDefinitionRepository extends JpaRepository<ProcessDefinitionEntity, Integer>, JpaSpecificationExecutor<ProcessDefinitionEntity> {
}