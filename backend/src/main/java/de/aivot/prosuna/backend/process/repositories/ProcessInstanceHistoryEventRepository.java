package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessInstanceHistoryEventRepository extends JpaRepository<ProcessInstanceEventEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEventEntity> {
}