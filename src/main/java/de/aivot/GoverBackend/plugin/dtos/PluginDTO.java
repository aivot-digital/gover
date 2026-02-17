package de.aivot.GoverBackend.plugin.dtos;

import de.aivot.GoverBackend.plugin.models.Plugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

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
        @Nonnull List<PluginComponentDTO> components
) {
    public static PluginDTO from(@Nonnull Plugin plugin,
                                 @Nonnull List<PluginComponentDTO> components) {
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
                components
        );
    }
}
