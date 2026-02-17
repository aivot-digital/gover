package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessInstanceAccessControlRepository extends JpaRepository<ProcessInstanceAccessControlEntity, Integer>, JpaSpecificationExecutor<ProcessInstanceAccessControlEntity> {
}