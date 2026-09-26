package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessAccessControlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessAccessControlRepository extends JpaRepository<ProcessAccessControlEntity, Integer>, JpaSpecificationExecutor<ProcessAccessControlEntity> {
}