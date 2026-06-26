package de.aivot.gover.backend.config.repositories;

import de.aivot.gover.backend.config.entities.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, String>, JpaSpecificationExecutor<SystemConfigEntity> {
}
