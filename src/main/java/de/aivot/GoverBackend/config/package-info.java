/**
 * This package contains all logic to handle all configurations of Gover.
 * This includes system-wide configurations, as well as user-specific configurations.
 *
 * <p>A configuration always needs a definition class extending the interface
 * {@link de.aivot.GoverBackend.config.models.SystemConfigDefinition} or
 * {@link de.aivot.GoverBackend.config.models.UserConfigDefinition}.
 * The configuration definition class is then used to create a configuration entity,
 * which can be stored in the database.</p>
 *
 * <p>Only admins can change system-wide configurations and other users' configurations.
 * Users who are not admins can only change their own configurations.</p>
 */
package de.aivot.GoverBackend.config;