package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessInstanceAccessControlPresetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessInstanceAccessControlPresetRepository extends JpaRepository<ProcessInstanceAccessControlPresetEntity, Integer>, JpaSpecificationExecutor<ProcessInstanceAccessControlPresetEntity> {
}