package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProcessNodeRepository extends JpaRepository<ProcessNodeEntity, Integer>, JpaSpecificationExecutor<ProcessNodeEntity> {
    List<ProcessNodeEntity> findAllByProcessId(Integer processDefinitionId);
}