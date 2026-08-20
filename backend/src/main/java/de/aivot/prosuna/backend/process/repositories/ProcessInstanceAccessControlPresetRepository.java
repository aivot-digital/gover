package de.aivot.prosuna.backend.process.repositories;

import de.aivot.prosuna.backend.process.entities.ProcessInstanceAccessControlPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessInstanceAccessControlPresetRepository extends JpaRepository<ProcessInstanceAccessControlPresetEntity, Integer>, JpaSpecificationExecutor<ProcessInstanceAccessControlPresetEntity> {
}