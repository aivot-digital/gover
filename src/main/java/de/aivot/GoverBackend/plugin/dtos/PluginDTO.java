package de.aivot.GoverBackend.plugin.dtos;

import de.aivot.GoverBackend.plugin.models.Plugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public record PluginDTO(
        @Nonnull String key,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull String buildDate,
        @Nonnull String version,
        @Nonnull String vendorName,
        @Nonnull String vendorWebsite,
        @Nonnull String changelog,
        @Nullable String deprecationNotice,
        @Nonnull List<List<PluginComponentDTO>> components
) {
    public static PluginDTO from(@Nonnull Plugin plugin,
                                 @Nonnull List<PluginComponentDTO> components) {
        List<List<PluginComponentDTO>> componentsGroups = components
                .stream()
                .collect(Collectors.groupingBy(PluginComponentDTO::componentKey))
                .values()
                .stream()
                .map(s -> s
                        .stream()
                        .sorted((c1, c2) -> Integer.compare(c2.majorVersion(), c1.majorVersion()))
                        .toList()
                )
                .toList();

        return new PluginDTO(
                plugin.getKey(),
                plugin.getName(),
                plugin.getDescription(),
                plugin.getBuildDate(),
                plugin.getVersion(),
                plugin.getVendorName(),
                plugin.getVendorWebsite(),
                plugin.getChangelog(),
                plugin.getDeprecationNotice(),
                componentsGroups
        );
    }
}
