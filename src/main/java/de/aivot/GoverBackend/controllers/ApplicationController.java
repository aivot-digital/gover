package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.models.dtos.*;
import de.aivot.GoverBackend.models.entities.*;
import de.aivot.GoverBackend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.Optional;

@RestController
public class ApplicationController {
    private final ApplicationRepository applicationRepository;
    private final AccessibleApplicationRepository accessibleApplicationRepository;
    private final AccessibleDepartmentRepository accessibleDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final DestinationRepository destinationRepository;
    private final ThemeRepository themeRepository;

    private final SubmissionRepository submissionRepository;

    @Autowired
    public ApplicationController(
            ApplicationRepository applicationRepository,
            AccessibleApplicationRepository accessibleApplicationRepository,
            AccessibleDepartmentRepository accessibleDepartmentRepository,
            DepartmentRepository departmentRepository,
            DestinationRepository destinationRepository,
            ThemeRepository themeRepository, SubmissionRepository submissionRepository) {
        this.applicationRepository = applicationRepository;
        this.accessibleApplicationRepository = accessibleApplicationRepository;
        this.accessibleDepartmentRepository = accessibleDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.destinationRepository = destinationRepository;
        this.themeRepository = themeRepository;
        this.submissionRepository = submissionRepository;
    }

    @GetMapping("/api/applications")
    public Collection<ApplicationListDto> list(
            Authentication authentication,
            @RequestParam(required = false) Integer department
    ) {
        User requester = (User) authentication.getPrincipal();

        Collection<Application> applications;
        if (requester.isAdmin()) {
            if (department != null) {
                applications = applicationRepository
                        .findAllByDevelopingDepartmentId(department);
            } else {
                applications = applicationRepository
                        .findAll();
            }
        } else {
            Collection<AccessibleApplication> accessibleApplications;
            if (department != null) {
                accessibleApplications = accessibleApplicationRepository
                        .findAllByKey_UserIdAndKey_DepartmentId(requester.getId(), department);
            } else {
                accessibleApplications = accessibleApplicationRepository
                        .findAllByKey_UserId(requester.getId());
            }

            var accessibleIds = accessibleApplications
                    .stream()
                    .map(AccessibleApplication::getKey)
                    .map(AccessibleApplication.AccessibleApplicationKey::getApplicationId)
                    .toList();
            applications = applicationRepository
                    .findAllByIdIn(accessibleIds);
        }

        return applications
                .stream()
                .map(ApplicationListDto::new)
                .toList();
    }

    @GetMapping("/api/public/applications")
    public Collection<ApplicationListPublicDto> listPublic() {
        return applicationRepository
                .findAllByStatus(ApplicationStatus.Published)
                .stream()
                .map(ApplicationListPublicDto::valueOf)
                .toList();
    }

    @PostMapping("/api/applications")
    public ApplicationDetailsMinimalDto create(
            Authentication authentication,
            @RequestBody ApplicationDetailsFullDto newApp
    ) {
        var requester = (User) authentication.getPrincipal();

        if (!requester.isAdmin()) {
            boolean membershipExists = accessibleDepartmentRepository
                    .existsByDepartmentIdAndUserId(newApp.getDevelopingDepartment(), requester.getId());
            if (!membershipExists) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        boolean exists = applicationRepository.existsBySlugAndVersion(newApp.getSlug(), newApp.getVersion());
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        boolean slugExists = applicationRepository.existsBySlug(newApp.getSlug());
        if (slugExists) {
            boolean inSameDepartment = applicationRepository.existsBySlugAndDevelopingDepartment_Id(newApp.getSlug(), newApp.getDevelopingDepartment());
            if (!inSameDepartment) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
        }

        var application = new Application();
        application.setSlug(newApp.getSlug());
        application.setVersion(newApp.getVersion());
        application.setTitle(newApp.getTitle());
        application.setStatus(newApp.getStatus());
        application.setRoot(newApp.getRoot());
        application.setSubmissionDeletionWeeks(newApp.getSubmissionDeletionWeeks());
        application.setCustomerAccessHours(newApp.getCustomerAccessHours());
        application.setOpenSubmissions(0);
        application.setInProgressSubmissions(0);
        application.setTotalSubmissions(0);

        if (newApp.getDestination() != null) {
            destinationRepository
                    .findById(newApp.getDestination())
                    .ifPresent(application::setDestination);
        }

        if (newApp.getLegalSupportDepartment() != null) {
            departmentRepository
                    .findById(newApp.getLegalSupportDepartment())
                    .ifPresent(application::setLegalSupportDepartment);
        }

        if (newApp.getTechnicalSupportDepartment() != null) {
            departmentRepository
                    .findById(newApp.getTechnicalSupportDepartment())
                    .ifPresent(application::setTechnicalSupportDepartment);
        }

        if (newApp.getImprintDepartment() != null) {
            departmentRepository
                    .findById(newApp.getImprintDepartment())
                    .ifPresent(application::setImprintDepartment);
        }

        if (newApp.getPrivacyDepartment() != null) {
            departmentRepository
                    .findById(newApp.getPrivacyDepartment())
                    .ifPresent(application::setPrivacyDepartment);
        }

        if (newApp.getAccessibilityDepartment() != null) {
            departmentRepository
                    .findById(newApp.getAccessibilityDepartment())
                    .ifPresent(application::setAccessibilityDepartment);
        }

        if (newApp.getDevelopingDepartment() != null) {
            departmentRepository
                    .findById(newApp.getDevelopingDepartment())
                    .ifPresent(application::setDevelopingDepartment);
        }

        if (newApp.getManagingDepartment() != null) {
            departmentRepository
                    .findById(newApp.getManagingDepartment())
                    .ifPresent(application::setManagingDepartment);
        }

        if (newApp.getResponsibleDepartment() != null) {
            departmentRepository
                    .findById(newApp.getResponsibleDepartment())
                    .ifPresent(application::setResponsibleDepartment);
        }

        if (newApp.getTheme() != null) {
            themeRepository
                    .findById(newApp.getTheme())
                    .ifPresent(application::setTheme);
        }

        var createdApplication = applicationRepository.save(application);
        return new ApplicationDetailsMinimalDto(createdApplication);
    }

    @GetMapping("/api/applications/{id}")
    public ApplicationDetailsFullDto retrieve(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        var requester = (User) authentication.getPrincipal();

        if (!requester.isAdmin()) {
            boolean membershipExists = accessibleApplicationRepository
                    .existsByKey_ApplicationIdAndKey_UserId(id, requester.getId());
            if (!membershipExists) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }

        return applicationRepository
                .findById(id)
                .map(ApplicationDetailsFullDto::new)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/api/public/applications/{slug}/{version}")
    public ApplicationDetailsFullDto retrievePublic(
            Authentication authentication,
            @PathVariable String slug,
            @PathVariable String version
    ) {
        var user = authentication != null ? (User) authentication.getPrincipal() : null;
        var applicationResult = applicationRepository.getBySlugAndVersion(slug, version);

        if (applicationResult.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        var application = applicationResult.get();

        if (application.getStatus() == ApplicationStatus.Published || user != null) {
            return new ApplicationDetailsFullDto(application);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/api/applications/{id}")
    public ApplicationDetailsFullDto update(
            Authentication authentication,
            @PathVariable Integer id,
            @RequestBody ApplicationDetailsFullDto updatedApp
    ) {
        var requester = (User) authentication.getPrincipal();
        var optApp = applicationRepository.findById(id);

        if (optApp.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var existingApp = optApp.get();

        if (existingApp.getStatus() == ApplicationStatus.Published && updatedApp.getStatus() != ApplicationStatus.Revoked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        if (!requester.isAdmin()) {
            boolean membershipExists = accessibleDepartmentRepository
                    .existsByDepartmentIdAndUserId(
                            existingApp.getDevelopingDepartment().getId(),
                            requester.getId()
                    );
            if (!membershipExists) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        if (existingApp.getStatus() == ApplicationStatus.Published && updatedApp.getStatus() == ApplicationStatus.Revoked) {
            existingApp.setStatus(ApplicationStatus.Revoked);
        } else if (existingApp.getStatus() == ApplicationStatus.Revoked && updatedApp.getStatus() == ApplicationStatus.Published) {
            existingApp.setStatus(ApplicationStatus.Published);
        } else {
            existingApp.setStatus(updatedApp.getStatus());
            existingApp.setRoot(updatedApp.getRoot());
            existingApp.setSubmissionDeletionWeeks(updatedApp.getSubmissionDeletionWeeks());
            existingApp.setCustomerAccessHours(updatedApp.getCustomerAccessHours());

            if (updatedApp.getDevelopingDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getDevelopingDepartment())
                        .ifPresent(existingApp::setDevelopingDepartment);
            }

            if (updatedApp.getDestination() != null) {
                destinationRepository
                        .findById(updatedApp.getDestination())
                        .ifPresent(existingApp::setDestination);
            } else {
                existingApp.setDestination(null);
            }

            if (updatedApp.getLegalSupportDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getLegalSupportDepartment())
                        .ifPresent(existingApp::setLegalSupportDepartment);
            } else {
                existingApp.setLegalSupportDepartment(null);
            }

            if (updatedApp.getTechnicalSupportDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getTechnicalSupportDepartment())
                        .ifPresent(existingApp::setTechnicalSupportDepartment);
            } else {
                existingApp.setTechnicalSupportDepartment(null);
            }

            if (updatedApp.getImprintDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getImprintDepartment())
                        .ifPresent(existingApp::setImprintDepartment);
            } else {
                existingApp.setImprintDepartment(null);
            }

            if (updatedApp.getPrivacyDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getPrivacyDepartment())
                        .ifPresent(existingApp::setPrivacyDepartment);
            } else {
                existingApp.setPrivacyDepartment(null);
            }

            if (updatedApp.getAccessibilityDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getAccessibilityDepartment())
                        .ifPresent(existingApp::setAccessibilityDepartment);
            } else {
                existingApp.setAccessibilityDepartment(null);
            }

            if (updatedApp.getManagingDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getManagingDepartment())
                        .ifPresent(existingApp::setManagingDepartment);
            } else {
                existingApp.setManagingDepartment(null);
            }

            if (updatedApp.getResponsibleDepartment() != null) {
                departmentRepository
                        .findById(updatedApp.getResponsibleDepartment())
                        .ifPresent(existingApp::setResponsibleDepartment);
            } else {
                existingApp.setResponsibleDepartment(null);
            }

            if (updatedApp.getTheme() != null) {
                themeRepository
                        .findById(updatedApp.getTheme())
                        .ifPresent(existingApp::setTheme);
            } else {
                existingApp.setTheme(null);
            }
        }

        var savedApplication = applicationRepository.save(existingApp);
        return new ApplicationDetailsFullDto(savedApplication);
    }

    @DeleteMapping("/api/applications/{id}")
    public void destroy(
            Authentication authentication,
            @PathVariable Integer id
    ) {
        var requester = (User) authentication.getPrincipal();

        Optional<Application> app = applicationRepository.findById(id);
        if (app.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!requester.isAdmin()) {
            boolean membershipExists = accessibleDepartmentRepository
                    .existsByDepartmentIdAndUserId(
                            app.get().getDevelopingDepartment().getId(),
                            requester.getId()
                    );
            if (!membershipExists) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }
        }

        if (submissionRepository.existsByApplication_IdAndArchivedIsNullAndIsTestSubmissionIsFalse(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        applicationRepository.delete(app.get());
    }

    @GetMapping("/api/public/max-file-size/{applicationId}")
    public MaxFileSizeDto getMaxFileSize(@PathVariable Integer applicationId) {
        Optional<Application> application = applicationRepository.findById(applicationId);

        MaxFileSizeDto maxFileSizeDto = new MaxFileSizeDto();
        maxFileSizeDto.setMaxFileSize(100);

        if (application.isPresent()) {
            Destination destination = application.get().getDestination();
            if (destination != null) {
                if (destination.getMaxAttachmentMegaBytes() != null) {
                    maxFileSizeDto.setMaxFileSize(destination.getMaxAttachmentMegaBytes());
                }
            }
        }

        return maxFileSizeDto;
    }
}
