package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.filters.DepartmentMembershipFilter;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.enums.UserRole;
import de.aivot.GoverBackend.exceptions.*;
import de.aivot.GoverBackend.form.cache.entities.FormLock;
import de.aivot.GoverBackend.form.dtos.FormDetailsResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormListResponseDTO;
import de.aivot.GoverBackend.form.dtos.FormRequestDTO;
import de.aivot.GoverBackend.form.entities.Form;
import de.aivot.GoverBackend.form.filters.FormWithMembershipFilter;
import de.aivot.GoverBackend.form.models.FormPublishChecklistItem;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormWithMembershipService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forms/")
public class FormController {
    private final ScopedAuditService auditService;

    private final FormMailService formMailService;
    private final ExceptionMailService exceptionMailService;
    private final FormService formService;
    private final FormWithMembershipService formWithMembershipService;
    private final DepartmentMembershipService departmentMembershipService;
    private final FormLockService formLockService;
    private final FormRevisionService formRevisionService;

    @Autowired
    public FormController(
            AuditService auditService,
            FormMailService formMailService,
            ExceptionMailService exceptionMailService,
            FormService formService,
            FormWithMembershipService formWithMembershipService,
            DepartmentMembershipService departmentMembershipService,
            FormLockService formLockService,
            FormRevisionService formRevisionService
    ) {
        this.auditService = auditService.createScopedAuditService(FormController.class);

        this.formMailService = formMailService;
        this.exceptionMailService = exceptionMailService;
        this.formService = formService;
        this.formWithMembershipService = formWithMembershipService;
        this.departmentMembershipService = departmentMembershipService;
        this.formLockService = formLockService;
        this.formRevisionService = formRevisionService;
    }

    @GetMapping("")
    public Page<FormListResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormWithMembershipFilter filter
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (filter.getUserId() != null) {
            return formWithMembershipService
                    .list(pageable, filter)
                    .map(FormListResponseDTO::fromEntity);
        } else {
            if (user.getGlobalAdmin()) {
                return formService
                        .list(pageable, filter.asFormFilter())
                        .map(FormListResponseDTO::fromEntity);
            } else {
                filter.setUserId(user.getId());
                return formWithMembershipService
                        .list(pageable, filter)
                        .map(FormListResponseDTO::fromEntity);
            }
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
        if (!user.getGlobalAdmin()) {
            if (departmentMembershipService.checkUserNotInDepartment(user, requestDTO.developingDepartmentId())) {
                throw ResponseException.forbidden("Die Mitarbeiter:in hat keinen Zugriff auf den Fachbereich.");
            }

            // TODO: Check user role
        }

        // Create the form based on the request DTO
        var createdForm = formService.create(requestDTO.toEntity());

        // Log the form creation
        auditService.logAction(user, AuditAction.Create, Form.class, Map.of(
                "formId", createdForm.getId(),
                "formSlug", createdForm.getSlug(),
                "formVersion", createdForm.getVersion(),
                "developingDepartmentId", createdForm.getDevelopingDepartmentId()
        ));

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
    @GetMapping("{formId}/")
    public FormDetailsResponseDTO retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Create a form filter
        var formSpec = FormWithMembershipFilter
                .create()
                .setId(formId)
                .setUserId(user.getId())
                .build();

        // Retrieve the form by its id
        return formWithMembershipService
                .retrieve(formSpec)
                .map(FormDetailsResponseDTO::fromEntity)
                .orElseThrow(ResponseException::notFound);
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
    @PutMapping("{formId}/")
    public FormDetailsResponseDTO update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @Valid @RequestBody FormRequestDTO requestDTO
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the department the form resides in
        if (!user.getGlobalAdmin()) {
            if (departmentMembershipService.checkUserNotInDepartment(user, requestDTO.developingDepartmentId())) {
                throw ResponseException.forbidden("Die Mitarbeiter:in hat keinen Zugriff auf den Fachbereich.");
            }

            // TODO: Check user role
        }

        // Check if the form is locked by another user
        checkFormLock(formId, user);

        // Fetch the existing form
        var existingForm = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        // Create a lock for the form
        try {
            formLockService
                    .create(new FormLock()
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

        // Update the form
        var updatedForm = formService.update(formId, requestDTO.toEntity());

        // Log the form update
        auditService.logAction(user, AuditAction.Update, Form.class, Map.of(
                "formId", updatedForm.getId(),
                "formSlug", updatedForm.getSlug(),
                "formVersion", updatedForm.getVersion(),
                "developingDepartmentId", updatedForm.getDevelopingDepartmentId()
        ));

        // Create a revision for the form
        formRevisionService.create(user, updatedForm, existingForm);

        // Return the form as a DTO
        return FormDetailsResponseDTO.fromEntity(updatedForm);
    }

    @GetMapping("{formId}/check-publish/")
    public List<FormPublishChecklistItem> checkPublish(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        return formService.getFormPublishChecklist(form);
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
    @PutMapping("{formId}/publish/")
    public FormDetailsResponseDTO publish(
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
        checkFormLock(formId, user);

        // Publish the form
        var publishedForm = formService.publish(form);

        // Log the form publication
        auditService.logAction(
                user,
                AuditAction.Update,
                Form.class,
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
        formRevisionService.create(user, publishedForm, form);

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
    @PutMapping("{formId}/revoke/")
    public FormDetailsResponseDTO revoke(
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
        if (!user.getGlobalAdmin()) {
            if (departmentMembershipService.checkUserNotInDepartment(user, form.getDevelopingDepartmentId())) {
                throw ResponseException.forbidden("User does not have access to the department.");
            }

            // TODO: Check user role
        }

        // Check if the form is locked by another user
        checkFormLock(formId, user);

        // Revoke the form
        var revokedForm = formService.revoke(form);

        // Log the form revocation
        auditService.logAction(
                user,
                AuditAction.Update,
                Form.class,
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
        formRevisionService.create(user, revokedForm, form);

        // Return the form as a DTO
        return FormDetailsResponseDTO.fromEntity(revokedForm);
    }

    /**
     * Delete a form by its id.
     * Forms can only be deleted by users who are members of the department the form resides in.
     * If the form is locked by another user, an exception is thrown.
     *
     * @param jwt    The authentication object.
     * @param formId The id of the form.
     */
    @DeleteMapping("{formId}/")
    public void delete(
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
        if (!user.getGlobalAdmin()) {
            if (departmentMembershipService.checkUserNotInDepartment(user, form.getDevelopingDepartmentId())) {
                throw ResponseException.forbidden("User does not have access to the department.");
            }

            // TODO: Check user role
        }

        // Check if the form is locked by another user
        checkFormLock(formId, user);

        // Delete the form
        var deletedForm = formService.delete(formId);

        auditService.logAction(
                user,
                AuditAction.Delete,
                Form.class,
                Map.of(
                        "formId", deletedForm.getId(),
                        "formSlug", deletedForm.getSlug(),
                        "formVersion", deletedForm.getVersion(),
                        "developingDepartmentId", deletedForm.getDevelopingDepartmentId()
                )
        );

        try {
            formMailService.sendDeleted(user, form);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.logException("Failed to send message about form deletion", e);
            exceptionMailService.send(e);
        }
    }

    /**
     * Check if a form is locked by another user.
     *
     * @param id   The id of the form.
     * @param user The user who wants to update the form.
     */
    private void checkFormLock(@Nonnull Integer id, UserEntity user) throws ResponseException {
        var existingFormLock = formLockService.retrieve(id);
        if (existingFormLock.isPresent()) {
            var formLockedByUserId = existingFormLock.get().getUserId();
            if (!user.hasId(formLockedByUserId)) {
                throw ResponseException.locked("Das Formular ist von einer anderen Mitarbeiter:in gesperrt.");
            }
        }
    }
}
