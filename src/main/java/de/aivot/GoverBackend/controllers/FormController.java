package de.aivot.GoverBackend.controllers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.aivot.GoverBackend.data.SystemConfigKey;
import de.aivot.GoverBackend.enums.ApplicationStatus;
import de.aivot.GoverBackend.enums.EntityLockState;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.models.auth.KeyCloakDetailsUser;
import de.aivot.GoverBackend.models.dtos.EntityLockDto;
import de.aivot.GoverBackend.models.dtos.MaxFileSizeDto;
import de.aivot.GoverBackend.models.entities.*;
import de.aivot.GoverBackend.models.lib.DiffItem;
import de.aivot.GoverBackend.models.xbezahldienst.XBezahldienstePaymentRequest;
import de.aivot.GoverBackend.redis.FormLockRepository;
import de.aivot.GoverBackend.repositories.*;
import de.aivot.GoverBackend.services.DiffService;
import de.aivot.GoverBackend.services.PaymentService;
import de.aivot.GoverBackend.services.PdfService;
import de.aivot.GoverBackend.services.ScriptService;
import de.aivot.GoverBackend.services.mail.ExceptionMailService;
import de.aivot.GoverBackend.services.mail.FormMailService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.exceptions.TemplateProcessingException;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class FormController {
    private final FormRepository formRepository;
    private final FormRevisionRepository formRevisionRepository;
    private final DepartmentMembershipRepository membershipRepository;
    private final DestinationRepository destinationRepository;
    private final SubmissionRepository submissionRepository;
    private final FormLockRepository formLockRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final FormMailService formMailService;
    private final ExceptionMailService exceptionMailService;
    private final PaymentService paymentService;
    private final PdfService pdfService;
    private final ThemeRepository themeRepository;
    private final DepartmentRepository departmentRepository;

    @Autowired
    public FormController(
            FormRepository formRepository,
            FormRevisionRepository formRevisionRepository,
            DepartmentMembershipRepository membershipRepository,
            DestinationRepository destinationRepository,
            SubmissionRepository submissionRepository,
            FormLockRepository formLockRepository,
            SystemConfigRepository systemConfigRepository,
            FormMailService formMailService,
            ExceptionMailService exceptionMailService,
            PaymentService paymentService,
            PdfService pdfService,
            ThemeRepository themeRepository, DepartmentRepository departmentRepository) {
        this.formRepository = formRepository;
        this.formRevisionRepository = formRevisionRepository;
        this.membershipRepository = membershipRepository;
        this.destinationRepository = destinationRepository;
        this.submissionRepository = submissionRepository;
        this.formLockRepository = formLockRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.formMailService = formMailService;
        this.exceptionMailService = exceptionMailService;
        this.paymentService = paymentService;
        this.pdfService = pdfService;
        this.themeRepository = themeRepository;
        this.departmentRepository = departmentRepository;
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

        // Create a new form and transfer the data from the DTO.
        var form = transferDataToExistingForm(new Form(), newForm);

        var savedApplication = formRepository.save(form);

        try {
            formMailService.sendAdded(user, savedApplication);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            exceptionMailService.send(e);
        }

        var diff = new DiffItem("/", null, new JSONObject(savedApplication).toMap());

        var formRevision = new FormRevision();
        formRevision.setFormId(savedApplication.getId());
        formRevision.setUserId(user.getId());
        formRevision.setTimestamp(LocalDateTime.now());
        formRevision.setDiff(List.of(diff));
        formRevisionRepository.save(formRevision);

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
    public FormPublicProjection retrievePublic(
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

    /**
     * Retrieve the latest published form by slug.
     *
     * @param slug The slug of the form to retrieve.
     * @return The form.
     */
    @GetMapping("/api/public/forms/{slug}")
    public FormPublicProjection retrievePublic(
            @PathVariable String slug
    ) {
        var version = formRepository
                .getLatestVersionBySlugAndStatus(slug, ApplicationStatus.Published)
                .orElseThrow(NotFoundException::new);
        return formRepository
                .getBySlugAndVersion(slug, version)
                .orElseThrow(NotFoundException::new);
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

        var existingAppJSON = new JSONObject(existingApp);

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
            existingApp = transferDataToExistingForm(existingApp, updatedApp);

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

        var updatedAppJSON = new JSONObject(existingApp);
        var changes = DiffService.createDiff(existingAppJSON, updatedAppJSON);

        if (!changes.isEmpty()) {
            var formRevision = new FormRevision();
            formRevision.setFormId(existingApp.getId());
            formRevision.setUserId(user.getId());
            formRevision.setTimestamp(LocalDateTime.now());
            formRevision.setDiff(changes);
            formRevisionRepository.save(formRevision);
        }

        return formRepository.save(existingApp);
    }

    @GetMapping("/api/forms/{formId}/revisions")
    public Page<FormRevision> listRevisions(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId,
            @RequestParam(required = false, name = "page", defaultValue = "0") Integer page,
            @RequestParam(required = false, name = "limit", defaultValue = "20") Integer limit
    ) {
        var user = KeyCloakDetailsUser.fromJwt(jwt);

        var app = formRepository
                .findById(formId)
                .orElseThrow(NotFoundException::new);

        boolean membershipExists = membershipRepository
                .existsByDepartmentIdAndUserId(app.getDevelopingDepartmentId(), user.getId());

        if (!membershipExists) {
            throw new ForbiddenException();
        }

        return formRevisionRepository
                .getAllByFormIdOrderByTimestampDesc(app.getId(), PageRequest.of(page, limit));
    }

    @GetMapping("/api/forms/{id}/revisions/rollback/{revisionId}")
    public Form rollbackRevision(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @PathVariable BigInteger revisionId
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

        var form = formRepository
                .findById(id)
                .orElseThrow(NotFoundException::new);

        var firstRevision = formRevisionRepository
                .getFirstByFormIdOrderByTimestampAsc(form.getId())
                .orElseThrow(NotFoundException::new);

        var targetRevisionToRollBackTo = formRevisionRepository
                .findById(revisionId)
                .orElseThrow(NotFoundException::new);

        if (firstRevision.getId().equals(targetRevisionToRollBackTo.getId())) {
            throw new BadRequestException();
        }

        var succeedingRevisionsToRollBack = formRevisionRepository
                .getAllByFormIdAndTimestampIsAfterOrderByTimestampDesc(form.getId(), targetRevisionToRollBackTo.getTimestamp());

        succeedingRevisionsToRollBack.add(targetRevisionToRollBackTo);

        var formObj = new JSONObject(form);
        for (var revision : succeedingRevisionsToRollBack) {
            for (var diff : revision.getDiff()) {
                formObj = DiffService.rollBackDiff(formObj, diff);
            }
        }

        // Remove these fields to prevent jackson from crashing because of LocalDateTime
        formObj.remove("updated");
        formObj.remove("created");

        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        var rolledBackForm = objectMapper.convertValue(formObj.toMap(), Form.class);
        return update(jwt, id, rolledBackForm);
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

    @PostMapping("/api/public/forms/{applicationId}/costs")
    public XBezahldienstePaymentRequest calculateCosts(
            @PathVariable Integer applicationId,
            @RequestBody Map<String, Object> customerData
    ) throws PaymentException {
        var form = formRepository
                .findById(applicationId)
                .orElseThrow(NotFoundException::new);

        return paymentService
                .createPaymentRequest(form, ScriptService.getEngine(), customerData, null)
                .orElseThrow(NotFoundException::new);
    }


    @GetMapping("/api/forms/{applicationId}/print")
    public ResponseEntity<Resource> getPrintableForm(
            @PathVariable Integer applicationId
    ) {
        var form = formRepository
                .findById(applicationId)
                .orElseThrow(NotFoundException::new);

        byte[] bytes;
        try {
            bytes = pdfService.generatePrintableForm(form);
        } catch (IOException | URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TemplateProcessingException e) {
            throw new ConflictException(e.getMessage());
        }

        Resource resource;
        try {
            resource = new ByteArrayResource(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(bytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(form.getSlug() + "-" + form.getVersion() + ".pdf")
                                .build()
                                .toString()
                )
                .body(resource);
    }

    private Form transferDataToExistingForm(Form existingForm, Form updatedForm) {
        // Check if a slug is already set, if so, the form already exists and can only be updated.
        if (existingForm.getSlug() == null) {
            existingForm.setSlug(updatedForm.getSlug());
            existingForm.setVersion(updatedForm.getVersion());
        }

        existingForm.setTitle(updatedForm.getTitle());

        existingForm.setStatus(updatedForm.getStatus());
        existingForm.setRoot(updatedForm.getRoot());
        existingForm.setSubmissionDeletionWeeks(updatedForm.getSubmissionDeletionWeeks());
        existingForm.setCustomerAccessHours(updatedForm.getCustomerAccessHours());

        if (updatedForm.getDestinationId() != null) {
            if (destinationRepository.existsById(updatedForm.getDestinationId())) {
                existingForm.setDestinationId(updatedForm.getDestinationId());
            } else {
                existingForm.setDestinationId(null);
            }
        } else {
            existingForm.setDestinationId(null);
        }

        existingForm.setLegalSupportDepartmentId(getExistingDepartmentId(updatedForm.getLegalSupportDepartmentId()).orElse(null));
        existingForm.setTechnicalSupportDepartmentId(getExistingDepartmentId(updatedForm.getTechnicalSupportDepartmentId()).orElse(null));
        existingForm.setImprintDepartmentId(getExistingDepartmentId(updatedForm.getImprintDepartmentId()).orElse(null));

        existingForm.setPrivacyDepartmentId(getExistingDepartmentId(updatedForm.getPrivacyDepartmentId()).orElse(null));
        existingForm.setAccessibilityDepartmentId(getExistingDepartmentId(updatedForm.getAccessibilityDepartmentId()).orElse(null));

        existingForm.setDevelopingDepartmentId(getExistingDepartmentId(updatedForm.getDevelopingDepartmentId()).orElseThrow(BadRequestException::new));

        existingForm.setManagingDepartmentId(getExistingDepartmentId(updatedForm.getManagingDepartmentId()).orElse(null));
        existingForm.setResponsibleDepartmentId(getExistingDepartmentId(updatedForm.getResponsibleDepartmentId()).orElse(null));

        if (updatedForm.getThemeId() != null) {
            if (themeRepository.existsById(updatedForm.getThemeId())) {
                existingForm.setThemeId(updatedForm.getThemeId());
            } else {
                existingForm.setThemeId(null);
            }
        } else {
            existingForm.setThemeId(null);
        }

        existingForm.setPdfBodyTemplateKey(updatedForm.getPdfBodyTemplateKey());
        existingForm.setProducts(updatedForm.getProducts());
        existingForm.setPaymentEndpointId(updatedForm.getPaymentEndpointId());
        existingForm.setPaymentOriginatorId(updatedForm.getPaymentOriginatorId());
        existingForm.setPaymentPurpose(updatedForm.getPaymentPurpose());
        existingForm.setPaymentDescription(updatedForm.getPaymentDescription());
        existingForm.setPaymentProvider(updatedForm.getPaymentProvider());

        if (Boolean.TRUE.equals(updatedForm.getBundIdEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BUNDID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            existingForm.setBundIdEnabled(true);
            existingForm.setBundIdLevel(updatedForm.getBundIdLevel());
        } else {
            existingForm.setBundIdEnabled(false);
            existingForm.setBundIdLevel(null);
        }

        if (Boolean.TRUE.equals(updatedForm.getBayernIdEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__BAYERN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            existingForm.setBayernIdEnabled(true);
            existingForm.setBayernIdLevel(updatedForm.getBayernIdLevel());
        } else {
            existingForm.setBayernIdEnabled(false);
            existingForm.setBayernIdLevel(null);
        }

        if (Boolean.TRUE.equals(updatedForm.getShIdEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__SCHLESWIG_HOLSTEIN_ID.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            existingForm.setShIdEnabled(true);
            existingForm.setShIdLevel(updatedForm.getShIdLevel());
        } else {
            existingForm.setShIdEnabled(false);
            existingForm.setShIdLevel(null);
        }

        if (Boolean.TRUE.equals(updatedForm.getMukEnabled()) && systemConfigRepository.existsByKeyAndValue(SystemConfigKey.NUTZERKONTEN__MUK.getKey(), SystemConfigKey.SYSTEM_CONFIG_TRUE)) {
            existingForm.setMukEnabled(true);
            existingForm.setMukLevel(updatedForm.getMukLevel());
        } else {
            existingForm.setMukEnabled(false);
            existingForm.setMukLevel(null);
        }

        return existingForm;
    }

    private Optional<Integer> getExistingDepartmentId(Integer departmentId) {
        if (departmentId != null && departmentRepository.existsById(departmentId)) {
            return Optional.of(departmentId);
        }
        return Optional.empty();
    }
}
