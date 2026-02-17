package de.aivot.GoverBackend.plugin.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A plugin that extends the application platform with additional features and functionalities.
 * A plugin can contain multiple plugin components.
 */
public interface Plugin {
    /**
     * The unique key identifying the plugin.
     * This should be a concise, lowercase string with words separated by hyphens (e.g., "my-plugin").
     * A good plugin key should include the vendor name and a descriptive name for the plugin to ensure uniqueness and clarity (e.g., "de.aivot.core").
     *
     * @return The plugin key.
     */
    @Nonnull
    String getKey();

    /**
     * The name of the plugin.
     * This will be displayed to users and should be descriptive and user-friendly.
     *
     * @return The plugin name.
     */
    @Nonnull
    String getName();

    /**
     * A brief description of the plugin.
     * This will be displayed to users and should provide a clear overview of the plugin's purpose and features.
     * You can use Markdown formatting in the description to enhance readability and presentation.
     *
     * @return The plugin description.
     */
    @Nonnull
    String getDescription();

    /**
     * The build date of the plugin in the ISO 8601 format.
     * This indicates when the plugin was built and can be useful for version tracking and debugging.
     *
     * @return The build date.
     */
    @Nonnull
    String getBuildDate();

    /**
     * The semantic version of the plugin.
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
     * This should be a concise and descriptive name that identifies the creator of the plugin.
     *
     * @return The vendor name.
     */
    @Nonnull
    String getVendorName();

    /**
     * The website of the vendor or author of the plugin.
     * This should be a valid URL that points to the vendor's website or profile, providing users with more information about the vendor and their other plugins or projects.
     *
     * @return The vendor website.
     */
    @Nonnull
    String getVendorWebsite();

    /**
     * The changelog of the plugin, describing the changes and updates made in each version.
     * This should be in Markdown format, following the "Keep a Changelog" format (<a href="https://keepachangelog.com/en/1.0.0/">...</a>).
     *
     * @return The plugin changelog.
     */
    @Nonnull
    String getChangelog();

    /**
     * An optional deprecation notice for the plugin, indicating that the plugin is deprecated and may be removed in future versions.
     * This should be in Markdown format, providing information about the deprecation and any recommended alternatives.
     * If the plugin is not deprecated, this method can return null.
     *
     * @return The deprecation notice, or null if the plugin is not deprecated.
     */
    @Nullable
    default String getDeprecationNotice() {
        return null;
    }
}
