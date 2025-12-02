package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.department.services.VDepartmentMembershipWithPermissionsService;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.exceptions.NoValidUserEMailsInDepartmentException;
import de.aivot.GoverBackend.form.dtos.FormExportDTO;
import de.aivot.GoverBackend.form.entities.FormEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionsEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.FormFilter;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsFilter;
import de.aivot.GoverBackend.form.filters.VFormWithPermissionsFilter;
import de.aivot.GoverBackend.form.services.*;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.mail.services.ExceptionMailService;
import de.aivot.GoverBackend.mail.services.FormMailService;
import de.aivot.GoverBackend.security.OpenAPISecurityConfiguration;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import de.aivot.GoverBackend.user.entities.UserEntity;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Form", description = "Interact with forms")
@SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
public class FormController {
    private final ScopedAuditService auditService;

    private final FormMailService formMailService;
    private final ExceptionMailService exceptionMailService;
    private final FormService formService;
    private final DepartmentMembershipService departmentMembershipService;
    private final FormLockService formLockService;
    private final FormRevisionService formRevisionService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final FormVersionService formVersionService;
    private final BuildProperties buildProperties;

    @Autowired
    public FormController(AuditService auditService,
                          FormMailService formMailService,
                          ExceptionMailService exceptionMailService,
                          FormService formService,
                          DepartmentMembershipService departmentMembershipService,
                          FormLockService formLockService,
                          FormRevisionService formRevisionService,
                          VFormWithPermissionsService vFormWithPermissionsService,
                          VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService,
                          VFormVersionWithDetailsService vFormVersionWithDetailsService, FormVersionService formVersionService, BuildProperties buildProperties) {
        this.auditService = auditService.createScopedAuditService(FormController.class);

        this.formMailService = formMailService;
        this.exceptionMailService = exceptionMailService;
        this.formService = formService;
        this.departmentMembershipService = departmentMembershipService;
        this.formLockService = formLockService;
        this.formRevisionService = formRevisionService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.formVersionService = formVersionService;
        this.buildProperties = buildProperties;
    }

    @GetMapping("")
    @Operation(
            summary = "List Forms",
            description = "List all forms the user has access to. " +
                          "Superadmins see all forms, staff users see only forms they have read permission for."
    )
    public Page<FormEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid FormFilter filter
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // If the user is a super admin, return all forms
        if (user.getSuperAdmin()) {
            return formService
                    .list(pageable, filter);
        }

        // Otherwise, return only forms the user has read permission for
        var vFilter = VFormWithPermissionsFilter
                .from(filter)
                .setUserId(user.getId())
                .setFormPermissionRead(true);

        return vFormWithPermissionsService
                .list(pageable, vFilter)
                .map(VFormWithPermissionsEntity::toFormEntity);
    }


    @PostMapping("")
    @Operation(
            summary = "Create Form",
            description = "Create a new form. " +
                          "The user must have create permission in the developing department of the form."
    )
    public FormEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody FormEntity newForm
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has permission to create forms in the developing department
        var departmentMembershipSpec = VDepartmentMembershipWithPermissionsFilter
                .create()
                .setDepartmentId(newForm.getDevelopingDepartmentId())
                .setUserId(user.getId())
                .setFormPermissionCreate(true)
                .build();
        var canCreate = user.getSuperAdmin() || vDepartmentMembershipWithPermissionsService
                .exists(departmentMembershipSpec);

        // If the user does not have permission, throw an exception
        if (!canCreate) {
            throw ResponseException.noPermission(PermissionLabels.FormPermissionCreate);
        }

        // Create the form
        var cratedForm = formService
                .create(newForm);

        // Write the audit log
        auditService.logAction(user, AuditAction.Create, FormEntity.class, Map.of(
                "id", cratedForm.getId(),
                "slug", cratedForm.getSlug(),
                "title", cratedForm.getInternalTitle(),
                "developingDepartmentId", cratedForm.getDevelopingDepartmentId()
        ));

        /*

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
         */

        // Create the initial revision for the form
        //formRevisionService.create(user, createdForm, null);

        // Save and return the application as a DTO.

        return cratedForm;
    }

    /**
     * Retrieve a form by its id.
     * Form retrieval is not limited to the user's department.
     *
     * @param jwt    The authentication object.
     * @param formId The id of the form.
     * @return The form as a DTO.
     */
/*
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

 */
    @GetMapping("{formId}/")
    @Operation(summary = "Retrieve Form", description = "Retrieve a form by its id.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public FormEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (user.getSuperAdmin()) {
            return formService
                    .retrieve(formId)
                    .orElseThrow(ResponseException::notFound);
        }

        var formGetWithPermissionSpec = VFormWithPermissionsFilter
                .create()
                .setId(formId)
                .setUserId(user.getId())
                .setFormPermissionRead(true)
                .build();

        return vFormWithPermissionsService
                .retrieve(formGetWithPermissionSpec)
                .orElseThrow(ResponseException::notFound)
                .toFormEntity();
    }

    @PutMapping("{formId}/")
    @Operation(summary = "Update Form", description = "Update a form by its id.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public FormEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @Valid @RequestBody FormEntity patchedForm
    ) throws ResponseException {
        // Extract staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the current user has edit permission for the form
        var canUpdateSpec = VDepartmentMembershipWithPermissionsFilter
                .create()
                .setDepartmentId(patchedForm.getDevelopingDepartmentId())
                .setUserId(user.getId())
                .setFormPermissionEdit(true)
                .build();
        var canUpdate = user.getSuperAdmin() ||
                        vDepartmentMembershipWithPermissionsService.exists(canUpdateSpec);
        if (!canUpdate) {
            throw ResponseException.noPermission("Formular Bearbeiten");
        }

        // Check if the form is locked by another user
        checkFormLock(formId, user, formLockService);

        // Update the form
        var updatedForm = formService
                .update(formId, patchedForm);

        // Log the form update
        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", updatedForm.getId(),
                "formSlug", updatedForm.getSlug(),
                "developingDepartmentId", updatedForm.getDevelopingDepartmentId()
        ));

        // Create a revision for the form
        // TODO: Create revision formRevisionService.create(user, form, existingForm);

        // Return the form as a DTO
        return updatedForm;
    }

    @PutMapping("{formId}/move/")
    @Operation(summary = "Move Form", description = "Move a form to another department.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
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
        var allVersions = vFormVersionWithDetailsService
                .list(VFormVersionWithDetailsFilter
                        .create()
                        .setFormId(formId));

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
    @Operation(summary = "Delete Form", description = "Delete a form by its id.")
    @SecurityRequirement(name = OpenAPISecurityConfiguration.SecurityName)
    public void destroy(
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
        if (user.getSuperAdmin()) {
            return;
        }

        // The user has access if they are a member of the developing department
        if (departmentMembershipService.checkUserInDepartment(user, developingDepartmentId)) {
            return;
        }

        throw ResponseException.forbidden("Sie haben keinen Zugriff auf das Formular, da Sie kein Mitglied des entwickelnden Fachbereichs sind.");
    }

    /**
     * @param formId
     * @param accessingUser
     * @param formLockService
     * @throws ResponseException
     * @deprecated use {@link FormLockService#checkFormLock(Integer, UserEntity)} instead
     */
    public static void checkFormLock(@Nonnull Integer formId,
                                     @Nonnull UserEntity accessingUser,
                                     @Nonnull FormLockService formLockService) throws ResponseException {
        formLockService.checkFormLock(formId, accessingUser);
    }


    @GetMapping("{formId}/export/")
    @Operation(
            summary = "Export Form",
            description = "Export a form by its id. " +
                          "You can optionally specify a version to export a specific version of the form. " +
                          "Superadmins can export any form, staff users can export forms they have read permission for."
    )
    public FormExportDTO export(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nullable @RequestParam(name = "version", required = false) Integer versionNumber
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead);
        }

        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound)

                .setId(0)
                .setDevelopingDepartmentId(0)
                .setDraftedVersion(null)
                .setPublishedVersion(null)
                .setVersionCount(0)
                .setCreated(LocalDateTime.now())
                .setUpdated(LocalDateTime.now());

        FormVersionEntity version;
        if (versionNumber != null) {
            version = formVersionService
                    .retrieve(new FormVersionEntityId(formId, versionNumber))
                    .orElseThrow(ResponseException::notFound);
        } else {
            version = formVersionService
                    .getLatestVersion(formId)
                    .orElseThrow(ResponseException::notFound);
        }

        version
                .setFormId(0)
                .setManagingDepartmentId(0)
                .setResponsibleDepartmentId(0)
                .setAccessibilityDepartmentId(0)
                .setPrivacyDepartmentId(0)
                .setImprintDepartmentId(0)
                .setLegalSupportDepartmentId(0)
                .setTechnicalSupportDepartmentId(0)
                .setThemeId(0)
                .setPdfTemplateKey(null)
                .setPaymentProviderKey(null)
                .setIdentityProviders(List.of())
                .setIdentityVerificationRequired(false)
                .setStatus(FormStatus.Drafted)
                .setCreated(LocalDateTime.now())
                .setUpdated(LocalDateTime.now())
                .setPublished(null)
                .setRevoked(null);

        ElementStreamUtils
                .applyAction(version.getRootElement(), element -> {
                    element.setName("");
                    element.setTestProtocolSet(null);
                });

        return new FormExportDTO(
                form,
                version,
                new FormExportDTO.Build(
                        buildProperties.getBuildVersion(),
                        buildProperties.getBuildNumber(),
                        buildProperties.getBuildTimestamp()
                ),
                LocalDateTime.now()
        );
    }
}
