package de.aivot.GoverBackend.process.repositories;

import de.aivot.GoverBackend.process.entities.ProcessTestConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessTestConfigRepository extends JpaRepository<ProcessTestConfigEntity, Integer>, JpaSpecificationExecutor<ProcessTestConfigEntity> {
}