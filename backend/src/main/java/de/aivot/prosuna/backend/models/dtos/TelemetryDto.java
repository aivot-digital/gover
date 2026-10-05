package de.aivot.prosuna.backend.models.dtos;

import java.util.Map;


public record TelemetryDto(
        Map<String, Object> prosunaConfig,
        Map<String, Object> storageConfig,
        Long forms,
        Long submissions,
        Long departments
) {
}
