package de.aivot.GoverBackend.models.dtos;

import java.util.Map;


public record TelemetryDto(
        Map<String, Object> goverConfig,
        Map<String, Object> storageConfig,
        Long forms,
        Long submissions,
        Long departments
) {
}
