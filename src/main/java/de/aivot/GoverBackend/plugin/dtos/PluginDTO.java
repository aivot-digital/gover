package de.aivot.GoverBackend.plugin.dtos;

import java.util.List;

public record PluginDTO(
    String name,
    String description,
    String buildDate,
    String version,
    String vendorName,
    List<PluginComponentDTO> components
) {
}
