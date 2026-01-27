package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessInstanceHistoryEventRepository extends JpaRepository<ProcessInstanceEventEntity, Long>, JpaSpecificationExecutor<ProcessInstanceEventEntity> {
}