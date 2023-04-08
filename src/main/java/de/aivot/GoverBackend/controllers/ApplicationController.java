package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.entities.Application;
import de.aivot.GoverBackend.models.entities.Department;
import de.aivot.GoverBackend.models.entities.User;
import de.aivot.GoverBackend.repositories.ApplicationRepository;
import de.aivot.GoverBackend.repositories.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@CrossOrigin
public class ApplicationController {
    private final ApplicationRepository applicationRepository;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public ApplicationController(ApplicationRepository applicationRepository, DepartmentRepository departmentRepository) {
        this.applicationRepository = applicationRepository;
        this.departmentRepository = departmentRepository;
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
}
