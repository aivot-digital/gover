package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.models.Application;
import de.aivot.GoverBackend.models.Department;
import de.aivot.GoverBackend.models.User;
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

    @GetMapping("/api/public/applications/{slug}/{version}")
    public Application getApplication(Authentication authentication, @PathVariable String slug, @PathVariable String version) {
        User user = authentication != null ? (User) authentication.getPrincipal() : null;
        Optional<Application> applicationResult = applicationRepository.getBySlugAndVersion(slug, version);

        if (applicationResult.isPresent()) {
            Application application = applicationResult.get();
            if (user != null || (int) application.getRoot().get("status") == 2) {
                return application;
            }
        }

        throw new ResourceNotFoundException();
    }

    @GetMapping("/api/public/departments/{id}")
    public Department getDepartment(@PathVariable Long id) {
        Optional<Department> department = departmentRepository.findById(id);

        if (department.isPresent()) {
            return department.get();
        }

        throw new ResourceNotFoundException();
    }
}
