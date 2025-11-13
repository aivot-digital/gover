package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.filters.DepartmentMembershipFilter;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.models.elements.RootElement;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.cache.entities.FormLockCacheEntity;
import de.aivot.GoverBackend.form.dtos.FormDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormListResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormRequestDTO;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.entities.FormVersionWithDetailsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.enums.FormType;
import de.aivot.GoverBackend.form.filters.FormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.filters.FormVersionWithMembershipFilter;
import de.aivot.GoverBackend.form.models.FormPublishChecklistItem;
import de.aivot.GoverBackend.form.services.*;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.mail.services.FormMailService;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forms/")
public class FormController {
    private final ScopedAuditService auditService;

    private final FormMailService formMailService;
    private final ExceptionMailService exceptionMailService;
    private final FormService formService;
    private final FormVersionWithMembershipService formVersionWithMembershipService;
    private final DepartmentMembershipService departmentMembershipService;
    private final FormLockService formLockService;
    private final FormRevisionService formRevisionService;
    private final FormVersionWithDetailsService formVersionWithDetailsService;
    private final FormVersionService formVersionService;
    private final FormWithMembershipService formWithMembershipService;

    @Autowired
    public FormController(AuditService auditService,
                          FormMailService formMailService,
                          ExceptionMailService exceptionMailService,
                          FormService formService,
                          FormVersionWithMembershipService formVersionWithMembershipService,
                          DepartmentMembershipService departmentMembershipService,
                          FormLockService formLockService,
                          FormRevisionService formRevisionService,
                          FormVersionWithDetailsService formVersionWithDetailsService,
                          FormVersionService formVersionService,
                          FormWithMembershipService formWithMembershipService) {
        this.auditService = auditService.createScopedAuditService(FormController.class);

        this.formMailService = formMailService;
        this.exceptionMailService = exceptionMailService;
        this.formService = formService;
        this.formVersionWithMembershipService = formVersionWithMembershipService;
        this.departmentMembershipService = departmentMembershipService;
        this.formLockService = formLockService;
        this.formRevisionService = formRevisionService;
        this.formVersionWithDetailsService = formVersionWithDetailsService;
        this.formVersionService = formVersionService;
        this.formWithMembershipService = formWithMembershipService;
    }

    @GetMapping("")
    public Page<FormListResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormVersionWithMembershipFilter filter
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user is a global admin
        if (user.getGlobalAdmin()) {
            // Check if a user id is provided in the filter to show only forms the given user has access to
            if (filter.getUserId() != null) {
                return formWithMembershipService
                        .list(pageable, filter.asFormWithMembershipFilter())
                        .map(FormListResponseDTO::fromEntity);
            }
            // If not, show all forms. Use the form service to prevent duplicates
            else {
                return formService
                        .list(pageable, filter.asFormFilter())
                        .map(FormListResponseDTO::fromEntity);
            }
        }
        // If the user is not a global admin, limit the forms to those in departments the user is a member of
        else {
            filter.setUserId(user.getId());
            return formWithMembershipService
                    .list(pageable, filter.asFormWithMembershipFilter())
                    .map(FormListResponseDTO::fromEntity);
        }
    }

    /**
     * Create a new form.
     * Forms can only be created for departments the user is a member of.
     *
     * @param jwt        The authentication object.
     * @param requestDTO The form request DTO containing the form data.
     * @return The form as a DTO.
     */
    @PostMapping("")
    public FormDetailsResponseDTO create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody FormRequestDTO requestDTO
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the department the form is being created in
        checkUserHasAccessToForm(user, requestDTO.developingDepartmentId(), departmentMembershipService);

        // create the form
        var createdFormEntity = formService
                .create(requestDTO.toFormEntity());
        auditService.logAction(user, AuditAction.Create, FormEntity.class, Map.of(
                "id", createdFormEntity.getId(),
                "slug", createdFormEntity.getSlug(),
                "title", createdFormEntity.getInternalTitle(),
                "developingDepartmentId", createdFormEntity.getDevelopingDepartmentId()
        ));

        // create the initial version
        var createdVersionEntity = formVersionService
                .create(
                        requestDTO
                                .toFormVersionEntity()
                                .setFormId(createdFormEntity.getId())
                );
        auditService.logAction(user, AuditAction.Create, FormVersionEntity.class, Map.of(
                "formId", createdVersionEntity.getFormId(),
                "version", createdVersionEntity.getVersion()
        ));

        createdFormEntity
                .setDraftedVersion(createdVersionEntity.getVersion());

        var createdForm = FormVersionWithDetailsEntity
                .of(createdFormEntity, createdVersionEntity);

        // Send a message about the form creation to the department
        try {
            formMailService.sendAdded(user, createdForm);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.logException("Failed to send message about form creation", e, Map.of(
                    "formId", createdForm.getId(),
                    "formSlug", createdForm.getSlug(),
                    "formVersion", createdForm.getVersion(),
                    "developingDepartmentId", createdForm.getDevelopingDepartmentId()
            ));
            exceptionMailService.send(e);
        }

        // Create the initial revision for the form
        formRevisionService.create(user, createdForm, null);

        // Save and return the application as a DTO.
        return FormDetailsResponseDTO.fromEntity(createdForm);
    }

    /**
     * Retrieve a form by its id.
     * Form retrieval is not limited to the user's department.
     *
     * @param jwt    The authentication object.
     * @param formId The id of the form.
     * @return The form as a DTO.
     */
    @GetMapping("{formId}/latest/")
    public FormDetailsResponseDTO retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Create a form filter
        var formFilter = FormVersionWithMembershipFilter
                .create()
                .setId(formId)
                .setUserId(user.getId());

        // Retrieve the form by its id
        return formVersionWithMembershipService
                .retrieveLatest(formFilter)
                .map(FormDetailsResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
    }

    /**
     * Retrieve a form by its id.
     * Form retrieval is not limited to the user's department.
     *
     * @param jwt    The authentication object.
     * @param formId The id of the form.
     * @return The form as a DTO.
     */
    @GetMapping("{formId}/{formVersion}/")
    public FormDetailsResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Create a form filter
        var formSpec = FormVersionWithDetailsFilter
                .create()
                .setId(formId)
                .setVersion(formVersion)
                .build();

        // Retrieve the form by its id
        var form = formVersionWithDetailsService
                .retrieve(formSpec)
                .orElseThrow(ResponseException::notFound);

        if (
                user.getGlobalAdmin() ||
                departmentMembershipService.checkUserInDepartment(user, form.getDevelopingDepartmentId()) ||
                departmentMembershipService.checkUserInDepartment(user, form.getManagingDepartmentId()) ||
                departmentMembershipService.checkUserInDepartment(user, form.getResponsibleDepartmentId())
        ) {
            return FormDetailsResponseDTO.fromEntity(form);
        } else {
            throw ResponseException.forbidden("Sie haben keinen Zugriff auf das Formular, da Sie kein Mitglied des entwickelnden Fachbereichs sind.");
        }
    }

    /**
     * Update a form by its id.
     * Forms can only be updated by users who are members of the department the form resides in.
     * The form is locked during the update process.
     * If the form is locked by another user, an exception is thrown.
     *
     * @param jwt        The authentication object.
     * @param formId     The id of the form.
     * @param requestDTO The form request DTO containing the form data.
     * @return The form as a DTO.
     */
    @PutMapping("{formId}/{formVersion}/")
    public FormDetailsResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion,
            @Nonnull @Valid @RequestBody FormRequestDTO requestDTO
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the department the form resides in
        checkUserHasAccessToForm(user, requestDTO.developingDepartmentId(), departmentMembershipService);

        // Check if the form is locked by another user
        checkFormLock(formId, user, formLockService);

        // Fetch the existing form
        var existingForm = formVersionWithDetailsService
                .retrieve(formId, formVersion)
                .orElseThrow(ResponseException::notFound);

        // Create a lock for the form
        try {
            formLockService
                    .create(new FormLockCacheEntity()
                            .setFormId(formId)
                            .setUserId(user.getId()));
        } catch (ResponseException e) {
            auditService.logException("Failed to create form lock", e, Map.of(
                    "formId", existingForm.getId(),
                    "formSlug", existingForm.getSlug(),
                    "formVersion", existingForm.getVersion(),
                    "developingDepartmentId", existingForm.getDevelopingDepartmentId()
            ));
            exceptionMailService.send(e);
            throw new RuntimeException(e);
        }

        // Convert the request DTO to an entity
        var formToUpdate = requestDTO
                .toFormEntity();
        var versionToUpdate = requestDTO
                .toFormVersionEntity();

        // Recalculate referenced IDs for all elements in the form
        ElementStreamUtils
                .applyAction(versionToUpdate.getRootElement(), BaseElement::recalculateReferencedIds);

        // Update the form
        var updatedForm = formService
                .update(formId, formToUpdate);
        var updatedVersion = formVersionService
                .update(FormVersionEntityId.of(formId, formVersion), versionToUpdate);

        // Log the form update
        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", updatedForm.getId(),
                "formSlug", updatedForm.getSlug(),
                "developingDepartmentId", updatedForm.getDevelopingDepartmentId()
        ));
        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", updatedVersion.getFormId(),
                "formVersion", updatedVersion.getVersion()
        ));

        var form = FormVersionWithDetailsEntity.of(updatedForm, updatedVersion);

        // Create a revision for the form
        formRevisionService.create(user, form, existingForm);

        // Return the form as a DTO
        return FormDetailsResponseDTO.fromEntity(form);
    }

    @PutMapping("{formId}/latest/as-new-version/")
    public FormDetailsResponseDTO latestVersionAsNewVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        // Check if the user has access to the department the form resides in
        checkUserHasAccessToForm(user, form.getDevelopingDepartmentId(), departmentMembershipService);

        var latestVersion = formVersionService
                .getLatestVersion(formId)
                .orElse(null);

        if (latestVersion != null) {
            if (latestVersion.getStatus() == FormStatus.Drafted) {
                var formVersionId = FormVersionEntityId
                        .of(formId, latestVersion.getVersion());
                formVersionService.delete(formVersionId);
            }
        } else {
            latestVersion = new FormVersionEntity(
                    form.getId(),
                    0,
                    FormStatus.Drafted,
                    "",
                    FormType.Public,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    new RootElement(),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                    null
            );
        }

        var newVersion = new FormVersionEntity(
                formId,
                null,
                FormStatus.Drafted,
                latestVersion.getPublicTitle(),
                latestVersion.getType(),
                latestVersion.getManagingDepartmentId(),
                latestVersion.getResponsibleDepartmentId(),
                latestVersion.getLegalSupportDepartmentId(),
                latestVersion.getTechnicalSupportDepartmentId(),
                latestVersion.getImprintDepartmentId(),
                latestVersion.getPrivacyDepartmentId(),
                latestVersion.getAccessibilityDepartmentId(),
                latestVersion.getDestinationId(),
                latestVersion.getCustomerAccessHours(),
                latestVersion.getSubmissionRetentionWeeks(),
                latestVersion.getThemeId(),
                latestVersion.getPdfTemplateKey(),
                latestVersion.getPaymentProviderKey(),
                latestVersion.getPaymentPurpose(),
                latestVersion.getPaymentDescription(),
                latestVersion.getPaymentProducts(),
                latestVersion.getIdentityProviders(),
                latestVersion.getIdentityVerificationRequired(),
                latestVersion.getRootElement(),
                null,
                null,
                null,
                null
        );

        var createdVersion = formVersionService
                .create(newVersion);

        return formVersionWithDetailsService
                .retrieve(formId, createdVersion.getVersion())
                .map(FormDetailsResponseDTO::fromEntity)
                .orElseThrow(() -> ResponseException.internalServerError("Fehler beim Laden der neuen Formularversion."));
    }

    @PutMapping("{formId}/{formVersion}/as-new-version/")
    public FormDetailsResponseDTO existingVersionAsNewVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formVersionWithDetailsService
                .retrieve(formId, formVersion)
                .orElseThrow(ResponseException::notFound);

        // Check if the user has access to the department the form resides in
        checkUserHasAccessToForm(user, form.getDevelopingDepartmentId(), departmentMembershipService);

        if (form.getDraftedVersion() != null) {
            var currentDraftedVersionId = FormVersionEntityId
                    .of(formId, form.getDraftedVersion());
            formVersionService.delete(currentDraftedVersionId);
        }

        var newVersion = new FormVersionEntity(
                formId,
                null,
                FormStatus.Drafted,
                form.getPublicTitle(),
                form.getType(),
                form.getManagingDepartmentId(),
                form.getResponsibleDepartmentId(),
                form.getLegalSupportDepartmentId(),
                form.getTechnicalSupportDepartmentId(),
                form.getImprintDepartmentId(),
                form.getPrivacyDepartmentId(),
                form.getAccessibilityDepartmentId(),
                form.getDestinationId(),
                form.getCustomerAccessHours(),
                form.getSubmissionRetentionWeeks(),
                form.getThemeId(),
                form.getPdfTemplateKey(),
                form.getPaymentProviderKey(),
                form.getPaymentPurpose(),
                form.getPaymentDescription(),
                form.getPaymentProducts(),
                form.getIdentityProviders(),
                form.getIdentityVerificationRequired(),
                form.getRootElement(),
                null,
                null,
                null,
                null
        );

        var createdVersion = formVersionService
                .create(newVersion);

        return formVersionWithDetailsService
                .retrieve(formId, createdVersion.getVersion())
                .map(FormDetailsResponseDTO::fromEntity)
                .orElseThrow(() -> ResponseException.internalServerError("Fehler beim Laden der neuen Formularversion."));
    }

    @GetMapping("{formId}/{formVersion}/check-publish/")
    public List<FormPublishChecklistItem> checkPublish(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formVersionWithDetailsService
                .retrieve(formId, formVersion)
                .orElseThrow(ResponseException::notFound);

        return formVersionWithDetailsService
                .getFormPublishChecklist(form);
    }

    /**
     * Publish a form by its id.
     * Forms can only be published by users who are members of the department the form resides in.
     * If the form is locked by another user, an exception is thrown.
     *
     * @param jwt    The authentication object.
     * @param formId The id of the form.
     * @return The form as a DTO.
     */
    @PutMapping("{formId}/{formVersion}/publish/")
    public FormDetailsResponseDTO publish(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Retrieve the form by its id
        var form = formVersionWithDetailsService
                .retrieve(formId, formVersion)
                .orElseThrow(ResponseException::notFound);

        // Check if the user has access to the department the form resides in and is allowed to publish the form
        if (!user.getGlobalAdmin()) {
            var spec = DepartmentMembershipFilter
                    .create()
                    .setDepartmentId(form.getDevelopingDepartmentId())
                    .setUserId(user.getId())
                    .build();

            var membership = departmentMembershipService
                    .retrieve(spec)
                    .orElseThrow(() -> ResponseException.forbidden("Die Mitarbeiter:in ist nicht Mitglied des Fachbereichs."));

            if (!(membership.getRole() == UserRole.Admin || membership.getRole() == UserRole.Publisher)) {
                throw ResponseException.forbidden("Die Mitarbeiter:in hat keine Berechtigung das Formular zu veröffentlichen.");
            }
        }

        // Check if the form is locked by another user
        checkFormLock(formId, user, formLockService);

        // Clone the form to preserve the original state
        var originalForm = form
                .clone();

        // Publish the form
        var publishedForm = formVersionWithDetailsService
                .publish(form);

        // Log the form publication
        auditService.logAction(
                user,
                AuditAction.Update,
                FormVersionEntity.class,
                Map.of(
                        "formId", form.getId(),
                        "formSlug", form.getSlug(),
                        "formVersion", form.getVersion(),
                        "developingDepartmentId", form.getDevelopingDepartmentId(),
                        "published", true
                )
        );

        // Send a message about the form publication
        try {
            formMailService.sendPublished(user, form);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.logException("Failed to send message about form publication", e, Map.of(
                    "formId", form.getId(),
                    "formSlug", form.getSlug(),
                    "formVersion", form.getVersion(),
                    "developingDepartmentId", form.getDevelopingDepartmentId()
            ));
            exceptionMailService.send(e);
        }

        // Create a revision for the form
        formRevisionService.create(user, publishedForm, originalForm);

        // Return the form as a DTO
        return FormDetailsResponseDTO.fromEntity(form);
    }

    /**
     * Revoke a form by its id.
     * Forms can only be revoked by users who are members of the department the form resides in.
     * If the form is locked by another user, an exception is thrown.
     *
     * @param jwt    The authentication object.
     * @param formId The id of the form.
     * @return The form as a DTO.
     */
    @PutMapping("{formId}/{formVersion}/revoke/")
    public FormDetailsResponseDTO revoke(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer formVersion
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Retrieve the form by its id
        var form = formVersionWithDetailsService
                .retrieve(formId, formVersion)
                .orElseThrow(ResponseException::notFound);

        // Check if the user has access to the department the form resides in
        if (!user.getGlobalAdmin()) {
            var spec = DepartmentMembershipFilter
                    .create()
                    .setDepartmentId(form.getDevelopingDepartmentId())
                    .setUserId(user.getId())
                    .build();

            var membership = departmentMembershipService
                    .retrieve(spec)
                    .orElseThrow(() -> ResponseException.forbidden("Die Mitarbeiter:in ist nicht Mitglied des Fachbereichs."));

            if (!(membership.getRole() == UserRole.Admin || membership.getRole() == UserRole.Publisher)) {
                throw ResponseException.forbidden("Die Mitarbeiter:in hat keine Berechtigung das Formular zu zurückzuziehen.");
            }
        }

        // Check if the form is locked by another user
        checkFormLock(formId, user, formLockService);

        // Clone the form to preserve the original state
        var originalForm = form
                .clone();

        // Revoke the form
        var revokedForm = formVersionWithDetailsService
                .revoke(form);

        // Log the form revocation
        auditService.logAction(
                user,
                AuditAction.Update,
                FormVersionEntity.class,
                Map.of(
                        "formId", form.getId(),
                        "formSlug", form.getSlug(),
                        "formVersion", form.getVersion(),
                        "developingDepartmentId", form.getDevelopingDepartmentId(),
                        "revoked", true
                )
        );

        try {
            formMailService.sendRevoked(user, form);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.logException("Failed to send message about form revocation", e, Map.of(
                    "formId", form.getId(),
                    "formSlug", form.getSlug(),
                    "formVersion", form.getVersion(),
                    "developingDepartmentId", form.getDevelopingDepartmentId()
            ));
            exceptionMailService.send(e);
        }

        // Create a revision for the form
        formRevisionService.create(user, revokedForm, originalForm);

        // Return the form as a DTO
        return FormDetailsResponseDTO.fromEntity(revokedForm);
    }

    /**
     * Move a form to another department.
     *
     * @param jwt                The authentication object.
     * @param formId             The id of the form.
     * @param targetDepartmentId The id of the target department.
     */
    @PutMapping("{formId}/move/")
    public void move(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @RequestParam Integer targetDepartmentId
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Retrieve the form by its id
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        // Check if the user has access to the department the form resides in
        checkUserHasAccessToForm(user, form.getDevelopingDepartmentId(), departmentMembershipService);

        // Check if the form is locked by another user
        checkFormLock(formId, user, formLockService);

        // Store the previous department id
        var previousDepartmentId = form.getDevelopingDepartmentId();

        // Move the form to the target department
        form.setDevelopingDepartmentId(targetDepartmentId);

        // Create a revision for the form
        formService.update(formId, form);

        // Get a list of all versions of the form
        var allVersions = formVersionWithDetailsService
                .list(FormVersionWithDetailsFilter.create().setFormId(formId));

        // Create a revision for each version of the form with the previous department id
        for (var version : allVersions) {
            var original = version
                    .clone()
                    .setDevelopingDepartmentId(previousDepartmentId);

            formRevisionService
                    .create(user, version, original);
        }
    }


    @DeleteMapping("{formId}/")
    public void deleteAll(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Retrieve the form by its id
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        // Check if the user has access to the department the form resides in
        checkUserHasAccessToForm(user, form.getDevelopingDepartmentId(), departmentMembershipService);

        // Check if the form is locked by another user
        checkFormLock(formId, user, formLockService);

        // Delete the form
        var deletedForm = formService.delete(formId);

        auditService.logAction(
                user,
                AuditAction.Delete,
                FormEntity.class,
                Map.of(
                        "formId", deletedForm.getId(),
                        "formSlug", deletedForm.getSlug(),
                        "developingDepartmentId", deletedForm.getDevelopingDepartmentId()
                )
        );

        try {
            formMailService.sendDeleted(user, deletedForm);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.logException("Failed to send message about form deletion", e);
            exceptionMailService.send(e);
        }
    }

    public static void checkUserHasAccessToForm(@Nonnull UserEntity user,
                                                @Nonnull Integer developingDepartmentId,
                                                @Nonnull DepartmentMembershipService departmentMembershipService) throws ResponseException {
        // The user has access if they are a global admin
        if (user.getGlobalAdmin()) {
            return;
        }

        // The user has access if they are a member of the developing department
        if (departmentMembershipService.checkUserInDepartment(user, developingDepartmentId)) {
            return;
        }

        throw ResponseException.forbidden("Sie haben keinen Zugriff auf das Formular, da Sie kein Mitglied des entwickelnden Fachbereichs sind.");
    }

    public static void checkFormLock(@Nonnull Integer formId,
                                     @Nonnull UserEntity accessingUser,
                                     @Nonnull FormLockService formLockService) throws ResponseException {
        var existingFormLock = formLockService.retrieve(formId);
        if (existingFormLock.isPresent()) {
            var formLockedByUserId = existingFormLock.get().getUserId();
            if (!accessingUser.hasId(formLockedByUserId)) {
                throw ResponseException
                        .locked("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}
