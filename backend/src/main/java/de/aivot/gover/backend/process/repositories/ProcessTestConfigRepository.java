package de.aivot.gover.backend.process.repositories;

import de.aivot.gover.backend.process.entities.ProcessTestConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProcessTestConfigRepository extends JpaRepository<ProcessTestConfigEntity, Integer>, JpaSpecificationExecutor<ProcessTestConfigEntity> {
}