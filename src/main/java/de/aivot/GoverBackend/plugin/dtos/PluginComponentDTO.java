package de.aivot.GoverBackend.plugin.dtos;

import de.aivot.GoverBackend.plugin.models.PluginComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PluginComponentDTO(
        @Nonnull String parentPluginKey,
        @Nonnull String componentKey,
        @Nonnull String key,
        @Nonnull String componentVersion,
        @Nonnull Integer majorVersion,
        @Nonnull String name,
        @Nonnull String description,
        @Nullable String deprecationNotice
) {
    public static PluginComponentDTO from(@Nonnull PluginComponent component) {
        return new PluginComponentDTO(
                component.getParentPluginKey(),
                component.getComponentKey(),
                component.getKey(),
                component.getComponentVersion(),
                component.getMajorVersion(),
                component.getName(),
                component.getDescription(),
                component.getDeprecationNotice()
        );
    }
}
