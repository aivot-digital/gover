package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.config.GoverConfig;
import de.aivot.GoverBackend.models.entities.*;
import de.aivot.GoverBackend.repositories.ApplicationRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import de.aivot.GoverBackend.repositories.DestinationRepository;
import de.aivot.GoverBackend.repositories.SystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Optional;

@RestController
@CrossOrigin
public class ApplicationController {
    private final ApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;
    private final DestinationRepository destinationRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final GoverConfig goverConfig;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository, DepartmentRepository departmentRepository, DestinationRepository destinationRepository, SystemConfigRepository systemConfigRepository, GoverConfig goverConfig) {
        this.applicationRepository = applicationRepository;
        this.departmentRepository = departmentRepository;
        this.destinationRepository = destinationRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.goverConfig = goverConfig;
    }

    /**
     * Fetch all published applications.
     * @return A collection of all published applications.
     */
    @GetMapping("/api/public/applications")
    public Collection<ListApplication> getApplicationList() {
        var applicationResult = applicationRepository.findPublishedApplications();
        return applicationResult.stream().map(ListApplication::new).toList();
    }

    /**
     * Fetch a single application based on the slug and version. Not published applications are only served to authenticated users.
     * @param authentication Optional authentication object of the current user.
     * @param slug The slug of the application.
     * @param version The version of the application
     * @return The desired application or a resource not found exception.
     */
    @GetMapping("/api/public/applications/{slug}/{version}")
    public Application getApplication(Authentication authentication, @PathVariable String slug, @PathVariable String version) {
        User user = authentication != null ? (User) authentication.getPrincipal() : null;
        var applicationResult = applicationRepository.getBySlugAndVersion(slug, version);

        if (applicationResult.isPresent()) {
            var application = applicationResult.get();
            if (user != null || (application.getStatus() == ApplicationStatus.Published)) {
                return application;
            }
        }

        throw new ResourceNotFoundException();
    }

    /**
     * Get a department by its id.
     * @param id The id of the department to fetch.
     * @return The desired department or a resource not found exception.
     */
    @GetMapping("/api/public/departments/{id}")
    public Department getDepartment(@PathVariable Long id) {
        var department = departmentRepository.findById(id);

        if (department.isPresent()) {
            return department.get();
        }

        throw new ResourceNotFoundException();
    }

    /**
     * Get the maximum allowed total file sizes of an application.
     * @param applicationId The id of the application.
     * @return The maximum allowed total file size.
     */
    @GetMapping("/api/public/max-file-size/{applicationId}")
    public Integer getMaxFileSize(@PathVariable Long applicationId) {
        Optional<Application> application = applicationRepository.findById(applicationId);

        if (application.isPresent()) {
            Integer destinationId = application.get().getRoot().getDestination();
            if (destinationId != null) {
                Optional<Destination> destination = destinationRepository.findById(Long.valueOf(destinationId));
                if (destination.isPresent()) {
                    if (destination.get().getMaxAttachmentMegaBytes() != null) {
                        return destination.get().getMaxAttachmentMegaBytes();
                    }
                }
            }
        }

        return 100;
    }

    /**
     * Get the sentry dns for the web app.
     * @return The sentry dns for the web app.
     */
    @GetMapping("/api/public/sentry-dns")
    public String getSentryDns() {
        return goverConfig.getSentryWebApp();
    }

    /**
     * Get the sentry dns for the web app.
     * @return The sentry dns for the web app.
     */
    @GetMapping("/api/public/system-configs")
    public Collection<SystemConfig> getPublicSystemConfigs() {
        return systemConfigRepository.findPublicSystemConfigs();
    }
}
