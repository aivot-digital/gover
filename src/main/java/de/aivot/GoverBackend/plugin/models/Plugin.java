package de.aivot.GoverBackend.plugin.models;

import jakarta.annotation.Nonnull;

/**
 * A plugin that extends the application platform with additional features and functionalities.
 * A plugin can contain multiple plugin components.
 */
public interface Plugin {
    /**
     * The unique key identifying the plugin.
     *
     * @return The plugin key.
     */
    @Nonnull
    String getKey();

    /**
     * The name of the plugin.
     *
     * @return The plugin name.
     */
    @Nonnull
    String getName();

    /**
     * A brief description of the plugin.
     *
     * @return The plugin description.
     */
    @Nonnull
    String getDescription();

    /**
     * The build date of the plugin in the ISO 8601 format.
     *
     * @return The build date.
     */
    @Nonnull
    String getBuildDate();

    /**
     * The version of the plugin.
     * This is the overall version.
     * Individual components may have their own versions.
     * This should be in compliance with Semantic Versioning (SemVer) format.
     *
     * @return The plugin version.
     */
    @Nonnull
    String getVersion();

    /**
     * The name of the vendor or author of the plugin.
     *
     * @return The vendor name.
     */
    @Nonnull
    String getVendorName();

    /**
     * The website of the vendor or author of the plugin.
     *
     * @return The vendor website.
     */
    @Nonnull
    String getVendorWebsite();
}

