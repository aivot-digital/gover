package de.aivot.prosuna.backend.plugin.dtos;

import de.aivot.prosuna.backend.plugin.enums.PluginComponentType;
import de.aivot.prosuna.backend.plugin.models.PluginComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public record PluginComponentDTO(
        @Nonnull String parentPluginKey,
        @Nonnull String componentKey,
        @Nonnull String key,
        @Nonnull String componentVersion,
        @Nonnull Integer majorVersion,
        @Nonnull PluginComponentType componentType,
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
                component.getComponentType(),
                component.getName(),
                component.getDescription(),
                component.getDeprecationNotice()
        );
    }
}
