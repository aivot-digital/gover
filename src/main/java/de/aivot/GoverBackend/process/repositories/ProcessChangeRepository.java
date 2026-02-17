package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessChangeRepository extends JpaRepository<ProcessChangeEntity, Long>, JpaSpecificationExecutor<ProcessChangeEntity> {
}