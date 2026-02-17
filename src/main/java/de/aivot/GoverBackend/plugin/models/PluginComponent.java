package de.aivot.GoverBackend.plugin.models;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * A component of a plugin that provides specific features or functionalities.
 * A plugin can contain multiple components.
 * <p>
 * Plugin components are identified by their parent plugin's key as well as their own key and version.
 * <p>
 * Plugin components are semantically versioned.
 * Minor and patch versions should not break the API of the component, while major versions may introduce breaking changes.
 * If you want to introduce a new major version of a plugin component, you should create a new component with the same parent plugin key and same component key.
 * The file should have a different name, for example "S3StorageInterfaceV2.java" for version 2 of the "S3StorageInterface" component.
 * This allows users to choose which version of the component they want to use, and also allows them to use multiple versions of the same component if needed.
 * <p>
 * The plugin management interface will display the component information, including the name, description, version, and deprecation notice (if applicable), to users when they view the plugin information.
 */
public interface PluginComponent {
    /**
     * The unique key identifying the parent plugin of this component.
     *
     * @return The parent plugin key.
     */
    @Nonnull
    String getParentPluginKey();

    /**
     * The unique key identifying this plugin component.
     *
     * @return The component key.
     */
    @Nonnull
    String getComponentKey();

    /**
     * The combination of the parent plugin key and the component key, which uniquely identifies this plugin component.
     * The format is "{parentPluginKey}.{componentKey}".
     * For example, if the parent plugin key is "com.example.myplugin" and the component key is "mycomponent", the resulting key would be "com.example.myplugin.mycomponent".
     *
     * @return The unique key for this plugin component.
     */
    @Nonnull
    default String getKey() {
        return String.format("%s.%s", getParentPluginKey(), getComponentKey());
    }

    /**
     * The semantic version of this plugin component.
     * The version should follow the format "MAJOR.MINOR.PATCH", where:
     * <ul>
     *  <li>MAJOR version is incremented for incompatible API changes,</li>
     *  <li>MINOR version is incremented for added functionality in a backwards-compatible manner,</li>
     *  <li>PATCH version is incremented for backwards-compatible bug fixes.</li>
     * </ul>
     *
     * @return The component semantic version.
     */
    @Nonnull
    String getComponentVersion();

    /**
     * Get the major version number of this plugin component.
     * This will be used to identify the component when using it in Gover.
     * The major version is the first part of the semantic version, before the first dot.
     * For example, if the component version is "2.1.0", the major version would be 2.
     *
     * @return The major version number of this plugin component.
     */
    @Nonnull
    default Integer getMajorVersion() {
        String[] versionParts = getComponentVersion().split("\\.");
        if (versionParts.length < 1) {
            throw new IllegalArgumentException("Invalid version format: " + getComponentVersion());
        }
        try {
            return Integer.parseInt(versionParts[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid major version format: " + versionParts[0], e);
        }
    }

    /**
     * Get the name of the plugin component.
     * This will be displayed to users when they view the plugin information in the plugin management interface.
     * It should be concise and descriptive, giving users a clear idea of what the component does and how it can
     * benefit them when using the plugin component.
     *
     * @return The component name.
     */
    @Nonnull
    String getName();

    /**
     * Get a brief description of the plugin component.
     * The description should provide an overview of the component's purpose and functionality, helping users
     * understand what the component does and how it can be used within the plugin.
     * You should use Markdown formatting in the description to enhance readability and provide a better user
     * experience when displaying the component information in the plugin management interface.
     *
     * @return The component description.
     */
    @Nonnull
    String getDescription();

    /**
     * Get an optional deprecation notice for this plugin component.
     * If the component is deprecated, this method should return a message indicating that the component is deprecated.
     * It should also provide information about the reason for deprecation and any recommended alternatives or migration paths for users who are currently using the component.
     * You can use Markdown formatting in the deprecation notice to enhance readability and provide a better user experience when displaying the component information in the plugin management interface.
     * If the component is not deprecated, this method should return null.
     *
     * @return An optional deprecation notice, or null if the component is not deprecated.
     */
    @Nullable
    default String getDeprecationNotice() {
        return null;
    }
}
