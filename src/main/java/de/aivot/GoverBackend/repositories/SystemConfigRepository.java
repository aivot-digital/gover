package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    Collection<SystemConfig> findSystemConfigsByPublicConfigIsTrue();
    Boolean existsByKeyAndValue(String systemConfigKey, String value);
}
