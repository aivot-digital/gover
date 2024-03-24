package de.aivot.GoverBackend.controllers;

import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.enums.EntityLockState;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.mail.ExceptionMailService;
import de.aivot.GoverBackend.mail.FormMailService;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.dtos.EntityLockDto;
import de.aivot.GoverBackend.models.dtos.MaxFileSizeDto;
import de.aivot.GoverBackend.models.entities.Form;
import de.aivot.GoverBackend.models.entities.FormListProjection;
import de.aivot.GoverBackend.models.entities.FormListProjectionPublic;
import de.aivot.GoverBackend.models.entities.FormLock;
import de.aivot.GoverBackend.redis.FormLockRepository;
import de.aivot.GoverBackend.repositories.*;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
public class FormController {
    private final FormRepository formRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final DestinationRepository destinationRepository;
    private final SubmissionRepository submissionRepository;
    private final FormLockRepository formLockRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final FormMailService formMailService;
    private final ExceptionMailService exceptionMailService;

    @Autowired
    public FormController(
            FormRepository formRepository,
            DepartmentMembershipRepository membershipRepository,
            DestinationRepository destinationRepository,
            SubmissionRepository submissionRepository,
            FormLockRepository formLockRepository,
            SystemConfigRepository systemConfigRepository,
            FormMailService formMailService,
            ExceptionMailService exceptionMailService
    ) {
        this.formRepository = formRepository;
        this.membershipRepository = membershipRepository;
        this.destinationRepository = destinationRepository;
        this.submissionRepository = submissionRepository;
        this.formLockRepository = formLockRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.formMailService = formMailService;
        this.exceptionMailService = exceptionMailService;
    }

    @GetMapping("/api/forms")
    public Collection<FormListProjection> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, name = "department") Integer departmentId,
            @RequestParam(required = false, name = "responsible") Integer responsibleDepartmentId,
            @RequestParam(required = false, name = "managing") Integer managingDepartmentId,
            @RequestParam(required = false, name = "destination") Integer destinationId,
            @RequestParam(required = false, name = "theme") Integer themeId,
            @RequestParam(required = false, name = "id") List<Integer> ids
    ) {
        Collection<FormListProjection> forms;
        if (departmentId != null) {
            forms = formRepository
                    .findAllByDevelopingDepartmentId(departmentId);
        } else if (responsibleDepartmentId != null) {
            forms = formRepository
                    .findAllByResponsibleDepartmentId(responsibleDepartmentId);
        } else if (managingDepartmentId != null) {
            forms = formRepository
                    .findAllByManagingDepartmentId(managingDepartmentId);
        } else if (destinationId != null) {
            forms = formRepository
                    .findAllByDestinationId(destinationId);
        } else if (themeId != null) {
            forms = formRepository
                    .findAllByThemeId(themeId);
        } else if (ids != null && !ids.isEmpty()) {
            forms = formRepository
                    .findAllByIdIn(ids);
        } else {
            forms = formRepository
                    .findAllByDevelopingDepartmentMemberUserId(
                            KeyCloakDetailsUser
                                    .fromJwt(jwt)
                                    .getId()
                    );
        }

        return forms;
    }

    /**
     * List all applications that are published.
     *
     * @return A collection of applications which are published.
     */
    @GetMapping("/api/public/forms")
    public Collection<FormListProjectionPublic> listPublic() {
        return formRepository
                .findAllPublicByStatus(ApplicationStatus.Published.getKey());
    }

    /**
     * Create a new application.
     *
     * @param jwt     The authentication object.
     * @param newForm The application to create.
     * @return The created application.
     */
    @PostMapping("/api/forms")
    public Form create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Form newForm
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);

        // Test if the user has access to the department.
        boolean hasAccessToDepartment = membershipRepository
                .existsByDepartmentIdAndUserId(
                        newForm.getDevelopingDepartmentId(),
                        user.getId()
                );
        if (!hasAccessToDepartment) {
            throw new ForbiddenException();
        }

        // Test if the application slug and version already exist.
        boolean exists = formRepository
                .existsBySlugAndVersion(
                        newForm.getSlug(),
                        newForm.getVersion()
                );
        if (exists) {
            throw new ConflictException();
        }

        // Test if the application slug already exists in another department.
        boolean slugExists = formRepository
                .existsBySlug(newForm.getSlug());
        if (slugExists) {
            boolean inSameDepartment = formRepository
                    .existsBySlugAndDevelopingDepartmentId(
                            newForm.getSlug(),
                            newForm.getDevelopingDepartmentId()
                    );
            if (!inSameDepartment) {
                throw new ConflictException();
            }
        }

        // Create a new application and transfer the data from the DTO.
        var application = new Form();
        application.setSlug(newForm.getSlug());
        application.setVersion(newForm.getVersion());
        application.setTitle(newForm.getTitle());
        application.setStatus(newForm.getStatus());
        application.setRoot(newForm.getRoot());
        application.setSubmissionDeletionWeeks(newForm.getSubmissionDeletionWeeks());
        application.setCustomerAccessHours(newForm.getCustomerAccessHours());
        application.setDestinationId(newForm.getDestinationId());
        application.setLegalSupportDepartmentId(newForm.getLegalSupportDepartmentId());
        application.setTechnicalSupportDepartmentId(newForm.getTechnicalSupportDepartmentId());
        application.setImprintDepartmentId(newForm.getImprintDepartmentId());
        application.setPrivacyDepartmentId(newForm.getPrivacyDepartmentId());
        application.setAccessibilityDepartmentId(newForm.getAccessibilityDepartmentId());
        application.setDevelopingDepartmentId(newForm.getDevelopingDepartmentId());
        application.setManagingDepartmentId(newForm.getManagingDepartmentId());
        application.setResponsibleDepartmentId(newForm.getResponsibleDepartmentId());
        application.setThemeId(newForm.getThemeId());
        application.setPdfBodyTemplateKey(newForm.getPdfBodyTemplateKey());


        if (Boolean.TRUE.equals(newForm.getBundIdEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BUNDID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            application.setBundIdEnabled(true);
            application.setBundIdLevel(newForm.getBundIdLevel());
        } else {
            application.setBundIdEnabled(false);
            application.setBundIdLevel(null);
        }

        if (Boolean.TRUE.equals(newForm.getBayernIdEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BAYERN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            application.setBayernIdEnabled(true);
            application.setBayernIdLevel(newForm.getBayernIdLevel());
        } else {
            application.setBayernIdEnabled(false);
            application.setBayernIdLevel(null);
        }

        if (Boolean.TRUE.equals(newForm.getShIdEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            application.setShIdEnabled(true);
            application.setShIdLevel(newForm.getShIdLevel());
        } else {
            application.setShIdEnabled(false);
            application.setShIdLevel(null);
        }

        if (Boolean.TRUE.equals(newForm.getMukEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__MUK.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            application.setMukEnabled(true);
            application.setMukLevel(newForm.getMukLevel());
        } else {
            application.setMukEnabled(false);
            application.setMukLevel(null);
        }

        var savedApplication = formRepository.save(application);

        try {
            formMailService.sendAdded(user, savedApplication);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            exceptionMailService.send(e);
        }

        // Save and return the application as a DTO.
        return savedApplication;
    }

    /**
     * Retrieve an application by ID.
     *
     * @param jwt The authentication object.
     * @param id  The ID of the application to retrieve.
     * @return The application.
     */
    @GetMapping("/api/forms/{id}")
    public Form retrieve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);

        var form = formRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        boolean hasAccessToApplication = membershipRepository
                .existsByDepartmentIdAndUserId(
                        form.getDevelopingDepartmentId(),
                        user.getId()
                );
        if (!hasAccessToApplication) {
            throw new ForbiddenException();
        }

        return form;
    }

    /**
     * Retrieve a published application by slug and version.
     *
     * @param jwt     The authentication object.
     * @param slug    The slug of the application to retrieve.
     * @param version The version of the application to retrieve.
     * @return The application.
     */
    @GetMapping("/api/public/forms/{slug}/{version}")
    public Form retrievePublic(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String slug,
            @PathVariable String version
    ) {
        var user = jwt != null ? KeyCloakDetailsUser.fromJwt(jwt) : null;
        var form = formRepository
                .getBySlugAndVersion(slug, version)
                .orElseThrow(NotFoundException::new);

        if (form.getStatus() == ApplicationStatus.Published || (user != null && Boolean.TRUE.equals(user.getEnabled()))) {
            return form;
        } else {
            throw new NotFoundException();
        }
    }

    @PutMapping("/api/forms/{id}")
    public Form update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody Form updatedApp
    ) {
        var user = KeyCloakDetailsUser.fromJwt(jwt);

        var formLock = formLockRepository
                .findById(id)
                .orElse(null);
        if (formLock != null && !formLock.getUserId().equals(user.getId())) {
            throw new EntityLockedException();
        }

        var existingApp = formRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        if (existingApp.getStatus() == ApplicationStatus.Published && updatedApp.getStatus() != ApplicationStatus.Revoked) {
            throw new ConflictException();
        }

        boolean membershipExists = membershipRepository
                .existsByDepartmentIdAndUserId(existingApp.getDevelopingDepartmentId(), user.getId());

        if (!membershipExists) {
            throw new ForbiddenException();
        }

        if (existingApp.getStatus() == ApplicationStatus.Published && updatedApp.getStatus() == ApplicationStatus.Revoked) {
            existingApp.setStatus(ApplicationStatus.Revoked);

            try {
                formMailService.sendRevoked(user, existingApp);
            } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
                exceptionMailService.send(e);
            }
        } else if (existingApp.getStatus() == ApplicationStatus.Revoked && updatedApp.getStatus() == ApplicationStatus.Published) {
            if ((Boolean.TRUE.equals(updatedApp.getBundIdEnabled()) && !systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BUNDID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) ||
                    (Boolean.TRUE.equals(updatedApp.getBayernIdEnabled()) && !systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BAYERN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) ||
                    (Boolean.TRUE.equals(updatedApp.getShIdEnabled()) && !systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) ||
                    (Boolean.TRUE.equals(updatedApp.getMukEnabled()) && !systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__MUK.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE))) {
                throw new NotAcceptableException();
            }

            existingApp.setStatus(ApplicationStatus.Published);

            try {
                formMailService.sendPublished(user, existingApp);
            } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
                exceptionMailService.send(e);
            }
        } else {
            existingApp.setStatus(updatedApp.getStatus());
            existingApp.setRoot(updatedApp.getRoot());
            existingApp.setSubmissionDeletionWeeks(updatedApp.getSubmissionDeletionWeeks());
            existingApp.setCustomerAccessHours(updatedApp.getCustomerAccessHours());
            existingApp.setDestinationId(updatedApp.getDestinationId());
            existingApp.setLegalSupportDepartmentId(updatedApp.getLegalSupportDepartmentId());
            existingApp.setTechnicalSupportDepartmentId(updatedApp.getTechnicalSupportDepartmentId());
            existingApp.setImprintDepartmentId(updatedApp.getImprintDepartmentId());
            existingApp.setPrivacyDepartmentId(updatedApp.getPrivacyDepartmentId());
            existingApp.setAccessibilityDepartmentId(updatedApp.getAccessibilityDepartmentId());
            existingApp.setDevelopingDepartmentId(updatedApp.getDevelopingDepartmentId());
            existingApp.setManagingDepartmentId(updatedApp.getManagingDepartmentId());
            existingApp.setResponsibleDepartmentId(updatedApp.getResponsibleDepartmentId());
            existingApp.setThemeId(updatedApp.getThemeId());
            existingApp.setPdfBodyTemplateKey(updatedApp.getPdfBodyTemplateKey());

            if (systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BUNDID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
                existingApp.setBundIdEnabled(updatedApp.getBundIdEnabled());
                existingApp.setBundIdLevel(updatedApp.getBundIdLevel());
            } else {
                existingApp.setBundIdEnabled(false);
                existingApp.setBundIdLevel(null);
            }

            if (systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BAYERN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
                existingApp.setBayernIdEnabled(updatedApp.getBayernIdEnabled());
                existingApp.setBayernIdLevel(updatedApp.getBayernIdLevel());
            } else {
                existingApp.setBayernIdEnabled(false);
                existingApp.setBayernIdLevel(null);
            }

            if (systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
                existingApp.setShIdEnabled(updatedApp.getShIdEnabled());
                existingApp.setShIdLevel(updatedApp.getShIdLevel());
            } else {
                existingApp.setShIdEnabled(false);
                existingApp.setShIdLevel(null);
            }

            if (systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__MUK.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
                existingApp.setMukEnabled(updatedApp.getMukEnabled());
                existingApp.setMukLevel(updatedApp.getMukLevel());
            } else {
                existingApp.setMukEnabled(false);
                existingApp.setMukLevel(null);
            }

            if (ApplicationStatus.Published.equals(existingApp.getStatus())) {
                try {
                    formMailService.sendPublished(user, existingApp);
                } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
                    exceptionMailService.send(e);
                }
            }
        }

        if (formLock == null) {
            formLock = new FormLock();
            formLock.setFormId(id);
            formLock.setUserId(user.getId());
            formLockRepository.save(formLock);
        } else {
            formLockRepository.save(formLock);
        }

        return formRepository.save(existingApp);
    }

    @DeleteMapping("/api/forms/{id}")
    public void destroy(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        var user = KeyCloakDetailsUser.fromJwt(jwt);

        var app = formRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        boolean membershipExists = membershipRepository
                .existsByDepartmentIdAndUserId(app.getDevelopingDepartmentId(), user.getId());

        if (!membershipExists) {
            throw new ForbiddenException();
        }

        if (submissionRepository.existsByFormIdAndArchivedIsNullAndIsTestSubmissionIsFalse(id)) {
            throw new ConflictException();
        }

        try {
            formMailService.sendDeleted(user, app);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            exceptionMailService.send(e);
        }

        formRepository.delete(app);
    }

    @GetMapping("/api/forms/{id}/lock")
    public EntityLockDto getLock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);
        var lock = formLockRepository
                .findById(id);

        if (lock.isEmpty()) {
            return new EntityLockDto(EntityLockState.Free, null);
        } else {
            if (lock.get().getUserId().equals(user.getId())) {
                return new EntityLockDto(EntityLockState.LockedSelf, user.getId());
            } else {
                return new EntityLockDto(EntityLockState.LockedOther, lock.get().getUserId());
            }
        }
    }

    @DeleteMapping("/api/forms/{id}/lock")
    public void deleteLock(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id
    ) {
        var user = KeyCloakDetailsUser
                .fromJwt(jwt);

        var lock = formLockRepository
                .findById(id);

        if (lock.isPresent()) {
            if (lock.get().getUserId().equals(user.getId())) {
                formLockRepository.delete(lock.get());
            }
        }
    }

    @GetMapping("/api/public/forms/{applicationId}/max-file-size")
    public MaxFileSizeDto getMaxFileSize(
            @PathVariable Integer applicationId
    ) {
        Optional<Form> application = formRepository.findById(applicationId);

        MaxFileSizeDto maxFileSizeDto = new MaxFileSizeDto();
        maxFileSizeDto.setMaxFileSize(100);

        if (application.isPresent() && application.get().getDestinationId() != null) {
            var destination = destinationRepository
                    .findById(application.get().getDestinationId());

            if (destination.isPresent() && destination.get().getMaxAttachmentMegaBytes() != null) {
                maxFileSizeDto.setMaxFileSize(destination.get().getMaxAttachmentMegaBytes());
            }
        }

        return maxFileSizeDto;
    }
}
