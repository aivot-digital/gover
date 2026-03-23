package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessRepository extends JpaRepository<ProcessEntity, Integer>, JpaSpecificationExecutor<ProcessEntity> {
}