package de.aivot.GoverBackend.models.dtos;

import de.aivot.GoverBackend.models.entities.SystemConfig;

import java.util.Collection;
import java.util.Map;


public record TelemetryDto(
        Map<String, Object> goverConfig,
        Map<String, Object> storageConfig,
        Collection<SystemConfig> systemConfigs,
        Long forms,
        Long submissions,
        Long departments
) {
}
