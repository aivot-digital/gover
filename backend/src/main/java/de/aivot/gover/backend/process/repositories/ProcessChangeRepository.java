package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessChangeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessChangeRepository extends JpaRepository<ProcessChangeEntity, Long>, JpaSpecificationExecutor<ProcessChangeEntity> {
}