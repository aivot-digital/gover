package de.aivot.GoverBackend.plugin.models;

import jakarta.annotation.Nonnull;

/**
 * A component of a plugin that provides specific features or functionalities.
 * A plugin can contain multiple components.
 * <p>
 * Plugin components are identified by their parent plugin's key as well as their own key and version.
 */
public interface PluginComponent {

    /**
     * The unique key identifying the plugin component.
     *
     * @return The component key.
     */
    @Nonnull
    String getKey();

    /**
     * The version of the plugin component.
     * This reflects major changes to the component.
     * Non-breaking changes should not increment this version.
     *
     * @return The component version.
     */
    @Nonnull
    Integer getVersion();

    /**
     * Get the versioned key of the plugin component, combining the key and version.
     *
     * @return The versioned component key.
     */
    @Nonnull
    default String getVersionedKey() {
        return String.format("%s__v%d", getKey(), getVersion());
    }

    /**
     * The unique key identifying the parent plugin of this component.
     *
     * @return The parent plugin key.
     */
    @Nonnull
    String getParentPluginKey();

    /**
     * Get the name of the plugin component.
     *
     * @return The component name.
     */
    @Nonnull
    String getName();

    /**
     * Get a brief description of the plugin component.
     *
     * @return The component description.
     */
    @Nonnull
    String getDescription();
}
