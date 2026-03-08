package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.models.AuditLogPayload;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.department.filters.VDepartmentMembershipWithPermissionsFilter;
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
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.system.properties.BuildProperties;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import de.aivot.GoverBackend.utils.StringUtils;
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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forms/")
@Tag(
        name = "Forms",
        description = "Forms are built for collecting data from users. " +
                "They can be designed with various elements and configurations to suit different data collection needs. " +
                "Forms can be published, managed, and analyzed within the system. " +
                "Forms are versioned with the „Form Version” resource."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class FormController {
    private final ScopedAuditService auditService;

    private final FormMailService formMailService;
    private final ExceptionMailService exceptionMailService;
    private final FormService formService;
    private final FormLockService formLockService;
    private final FormRevisionService formRevisionService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final FormVersionService formVersionService;
    private final BuildProperties buildProperties;
    private final UserService userService;

    @Autowired
    public FormController(AuditService auditService,
                          FormMailService formMailService,
                          ExceptionMailService exceptionMailService,
                          FormService formService,
                          FormLockService formLockService,
                          FormRevisionService formRevisionService,
                          VFormWithPermissionsService vFormWithPermissionsService,
                          VDepartmentMembershipWithPermissionsService vDepartmentMembershipWithPermissionsService,
                          VFormVersionWithDetailsService vFormVersionWithDetailsService,
                          FormVersionService formVersionService,
                          BuildProperties buildProperties,
                          UserService userService) {
        this.auditService = auditService.createScopedAuditService(FormController.class, "Formulare");

        this.formMailService = formMailService;
        this.exceptionMailService = exceptionMailService;
        this.formService = formService;
        this.formLockService = formLockService;
        this.formRevisionService = formRevisionService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.vDepartmentMembershipWithPermissionsService = vDepartmentMembershipWithPermissionsService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.formVersionService = formVersionService;
        this.buildProperties = buildProperties;
        this.userService = userService;
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
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // If the user is a super admin, return all forms
        if (user.getIsSuperAdmin()) {
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
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has permission to create forms in the developing department
        var departmentMembershipSpec = VDepartmentMembershipWithPermissionsFilter
                .create()
                .setDepartmentId(newForm.getDevelopingDepartmentId())
                .setUserId(user.getId())
                .setFormPermissionCreate(true)
                .build();
        var canCreate = user.getIsSuperAdmin() || vDepartmentMembershipWithPermissionsService
                .exists(departmentMembershipSpec);

        // If the user does not have permission, throw an exception
        if (!canCreate) {
            throw ResponseException.noPermission(PermissionLabels.FormPermissionCreate);
        }

        // Create the form
        var cratedForm = formService
                .create(newForm);

        // Write the audit log
        auditService.create()
                .withUser(user)
                .withAuditAction(AuditAction.Create, FormEntity.class,
                        cratedForm.getId(),
                        "id",
                        Map.of(
                                "id", cratedForm.getId(),
                                "slug", cratedForm.getSlug(),
                                "title", cratedForm.getInternalTitle(),
                                "developingDepartmentId", cratedForm.getDevelopingDepartmentId()
                        ))
                .withMessage(
                        "Das Formular %s (Slug %s, ID %s) wurde von der Mitarbeiter:in %s erstellt.",
                        StringUtils.quote(cratedForm.getInternalTitle()),
                        StringUtils.quote(cratedForm.getSlug()),
                        StringUtils.quote(String.valueOf(cratedForm.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        return cratedForm;
    }

    @GetMapping("{formId}/")
    @Operation(
            summary = "Retrieve Form",
            description = "Retrieve a form by its id. " +
                    "Superadmins can retrieve any form, staff users can retrieve forms they have read permission for."
    )
    @SecurityRequirement(name = OpenApiConfiguration.Security)
    public FormEntity retrieve(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (user.getIsSuperAdmin()) {
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
    @Operation(
            summary = "Update Form",
            description = "Update a form by its id." +
                    "Super admins can update any form, staff users can update forms they have edit permission for."
    )
    @SecurityRequirement(name = OpenApiConfiguration.Security)
    public FormEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @Valid @RequestBody FormEntity patchedForm
    ) throws ResponseException {
        // Extract staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the current user has edit permission for the form
        var canUpdateSpec = VDepartmentMembershipWithPermissionsFilter
                .create()
                .setDepartmentId(patchedForm.getDevelopingDepartmentId())
                .setUserId(user.getId())
                .setFormPermissionEdit(true)
                .build();
        var canUpdate = user.getIsSuperAdmin() ||
                vDepartmentMembershipWithPermissionsService.exists(canUpdateSpec);
        if (!canUpdate) {
            throw ResponseException.noPermission(PermissionLabels.FormPermissionEdit);
        }

        // Check if the form is locked by another user
        formLockService.checkFormLock(formId, user);

        // Update the form
        var updatedForm = formService
                .update(formId, patchedForm);

        // Log the form update
        auditService.create().withUser(user).withAuditAction(AuditAction.Update, FormEntity.class, updatedForm.getId(), "formId", Map.of(
                "formId", updatedForm.getId(),
                "formSlug", updatedForm.getSlug(),
                "developingDepartmentId", updatedForm.getDevelopingDepartmentId()
        ))
                .withMessage(
                        "Das Formular %s (Slug %s, ID %s) wurde von der Mitarbeiter:in %s aktualisiert.",
                        StringUtils.quote(updatedForm.getInternalTitle()),
                        StringUtils.quote(updatedForm.getSlug()),
                        StringUtils.quote(String.valueOf(updatedForm.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        // TODO: Create revision formRevisionService.create(user, form, existingForm);

        return updatedForm;
    }

    @PutMapping("{formId}/move/")
    @Operation(
            summary = "Move Form",
            description = "Move a form to another department. " +
                    "The user must be a super admin or have edit permission in the current developing department of the form."
    )
    @SecurityRequirement(name = OpenApiConfiguration.Security)
    public void move(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @RequestParam Integer targetDepartmentId
    ) throws ResponseException {
        // Extract staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has edit permission for the form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionEdit,
                    PermissionLabels.FormPermissionEdit);
        }

        // Retrieve the form by its id
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        // Check if the form is locked by another user
        formLockService.checkFormLock(formId, user);

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
    @Operation(
            summary = "Delete Form",
            description = "Delete a form by its id. " +
                    "Super admins can delete any form, staff users can delete forms they have delete permission for."
    )
    @SecurityRequirement(name = OpenApiConfiguration.Security)
    public void destroy(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Extract staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has edit permission for the form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionEdit,
                    PermissionLabels.FormPermissionEdit);
        }

        // Retrieve the form by its id
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        // Check if the form is locked by another user
        formLockService.checkFormLock(formId, user);

        // Delete the form
        var deletedForm = formService.delete(formId);

        auditService.create().withUser(user).withAuditAction(AuditAction.Delete, FormEntity.class, deletedForm.getId(), "formId", Map.of(
                        "formId", deletedForm.getId(),
                        "formSlug", deletedForm.getSlug(),
                        "developingDepartmentId", deletedForm.getDevelopingDepartmentId()
                ))
                .withMessage(
                        "Das Formular %s (Slug %s, ID %s) wurde von der Mitarbeiter:in %s gelöscht.",
                        StringUtils.quote(deletedForm.getInternalTitle()),
                        StringUtils.quote(deletedForm.getSlug()),
                        StringUtils.quote(String.valueOf(deletedForm.getId())),
                        StringUtils.quote(user.getFullName())
                )
                .log();

        try {
            formMailService.sendDeleted(user, deletedForm);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.create()
                    .withUser(user)
                    .setTriggerType("Exception")
                    .setMessage("Failed to send message about form deletion")
                    .setMetadata(Map.of(
                            "exceptionType", e.getClass().getName(),
                            "formId", deletedForm.getId(),
                            "formSlug", deletedForm.getSlug()
                    )).log();
            exceptionMailService.send(e);
        }
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
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (!user.getIsSuperAdmin()) {
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
