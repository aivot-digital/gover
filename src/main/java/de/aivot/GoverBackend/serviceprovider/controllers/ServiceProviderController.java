package de.aivot.GoverBackend.serviceprovider.controllers;

import de.aivot.GoverBackend.serviceprovider.dtos.ServiceProviderDTO;
import de.aivot.GoverBackend.serviceprovider.providers.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Controller for service providers.
 * This is used to send information about the available service providers to the UI via the REST API.
 */
@RestController
public class ServiceProviderController {
    private final List<ServiceProvider> serviceProviders;

    @Autowired
    public ServiceProviderController(List<ServiceProvider> serviceProviders) {
        this.serviceProviders = serviceProviders;
    }

    /**
     * List all available service providers.
     * @return A list of all available service providers
     */
    @GetMapping("/api/service-providers")
    public Collection<ServiceProviderDTO> list() {
        return serviceProviders
                .stream()
                .sorted(Comparator.comparing(ServiceProvider::getPackageName))
                .map(ServiceProviderDTO::fromSPI)
                .toList();
    }
}
