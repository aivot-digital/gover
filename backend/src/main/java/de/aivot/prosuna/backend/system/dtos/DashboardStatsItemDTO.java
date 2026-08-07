package de.aivot.prosuna.backend.system.dtos;

public record DashboardStatsItemDTO(
        String id,
        String title,
        String subtitle,
        Number value,
        String href
) {
}
