package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessDefinitionChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessDefinitionChangeRepository extends JpaRepository<ProcessDefinitionChangeEntity, Long>, JpaSpecificationExecutor<ProcessDefinitionChangeEntity> {
}