package de.aivot.gover.backend.system.dtos;

public record DashboardStatsItemDTO(
        String id,
        String title,
        String subtitle,
        Number value,
        String href
) {
}
