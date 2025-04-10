/**
 * This package contains classes and interfaces related to the management of secrets within the GoverBackend application.
 *
 * <p>
 * The main components of this package include:
 * </p>
 * <ul>
 *   <li>{@link de.aivot.GoverBackend.secrets.controllers.SecretController} - REST controller for handling API requests related to secrets.</li>
 *   <li>{@link de.aivot.GoverBackend.secrets.services.SecretService} - Service class for business logic related to secrets, including encryption and decryption.</li>
 *   <li>{@link de.aivot.GoverBackend.secrets.entities.SecretEntity} - Entity class representing a secret in the database.</li>
 *   <li>{@link de.aivot.GoverBackend.secrets.repositories.SecretRepository} - Repository interface for CRUD operations on secrets.</li>
 *   <li>{@link de.aivot.GoverBackend.secrets.properties.SecretConfigurationProperties} - Configuration properties for secret management.</li>
 * </ul>
 *
 * <p>
 * The package also includes utility classes and DTOs for handling secret data transfer and validation.
 * </p>
 */
package de.aivot.GoverBackend.secrets;