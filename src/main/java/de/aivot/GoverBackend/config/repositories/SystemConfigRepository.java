package de.aivot.GoverBackend.config.repositories;

import de.aivot.GoverBackend.config.entities.SystemConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, String>, JpaSpecificationExecutor<SystemConfigEntity> {
}
