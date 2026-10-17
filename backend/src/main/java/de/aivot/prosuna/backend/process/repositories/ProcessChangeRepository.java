package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessChangeRepository extends JpaRepository<ProcessChangeEntity, Long>, JpaSpecificationExecutor<ProcessChangeEntity> {
}