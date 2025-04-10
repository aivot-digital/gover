package de.aivot.GoverBackend.serviceprovider.providers;

/**
 * Interface for service providers.
 * Implement this interface to provide a service to the application.
 */
public interface ServiceProvider {
    /**
     * The unique identifier of the provider
     * @return The unique identifier of the provider
     */
    String getPackageName();

    /**
     * The label of the provider when displayed in the UI
     * @return The label of the provider
     */
    String getLabel();

    /**
     * The description of the provider when displayed in the UI
     * @return The description of the provider
     */
    String getDescription();
}
