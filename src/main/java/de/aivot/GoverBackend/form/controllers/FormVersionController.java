package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.elements.models.elements.BaseElement;
import de.aivot.GoverBackend.elements.utils.ElementStreamUtils;
import de.aivot.GoverBackend.form.cache.entities.FormLockCacheEntity;
import de.aivot.GoverBackend.form.entities.*;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.FormVersionFilter;
import de.aivot.GoverBackend.form.filters.VFormVersionWithDetailsAndPermissionFilter;
import de.aivot.GoverBackend.form.models.FormPublishChecklistItem;
import de.aivot.GoverBackend.form.services.*;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenApiConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/form-versions/")
@Tag(
        name = "Form Versions",
        description = "Form versions represent different iterations of a form. " +
                      "They allow for version control of a form over time. " +
                      "Versions can be published and revoken but only one version can be published at a time."
)
@SecurityRequirement(name = OpenApiConfiguration.Security)
public class FormVersionController {
    private final ScopedAuditService auditService;

    private final FormVersionService formVersionService;
    private final FormService formService;
    private final FormLockService formLockService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService;
    private final FormRevisionService formRevisionService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final UserService userService;

    @Autowired
    public FormVersionController(AuditService auditService,
                                 FormVersionService formVersionService,
                                 FormService formService,
                                 FormLockService formLockService,
                                 VFormWithPermissionsService vFormWithPermissionsService,
                                 VFormVersionWithDetailsAndPermissionsService vFormVersionWithDetailsAndPermissionsService,
                                 FormRevisionService formRevisionService,
                                 VFormVersionWithDetailsService vFormVersionWithDetailsService,
                                 UserService userService) {
        this.auditService = auditService.createScopedAuditService(FormVersionController.class);
        this.formVersionService = formVersionService;
        this.formService = formService;
        this.formLockService = formLockService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.vFormVersionWithDetailsAndPermissionsService = vFormVersionWithDetailsAndPermissionsService;
        this.formRevisionService = formRevisionService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List Form Versions",
            description = "List all form versions with pagination and filtering. " +
                          "Super admins can see all form versions, while regular users only see versions they have read access to."
    )
    public Page<FormVersionEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @ParameterObject @Valid FormVersionFilter filter
    ) throws ResponseException {
        // Determine the user for permission checks
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        if (user.getIsSuperAdmin()) {
            return formVersionService
                    .list(pageable, filter);
        }

        // Otherwise, filter form versions based on user permissions
        var vFilter = VFormVersionWithDetailsAndPermissionFilter
                .from(filter)
                .setUserId(user.getId())
                .setFormPermissionRead(true);

        // List all forms the user has read access to
        return vFormVersionWithDetailsAndPermissionsService
                .list(pageable, vFilter)
                .map(VFormVersionWithDetailsAndPermissionsEntity::toFormVersionEntity);
    }

    @PostMapping("")
    @Operation(
            summary = "Create Form Version",
            description = "Create a new version for a form. " +
                          "Requires the system role super admin or edit permissions on the parent form."
    )
    public FormVersionEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody FormVersionEntity newFormVersion
    ) throws ResponseException {
        // Determine the user to check permissions
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Get the version of the parent form to check permissions for
        var formId = newFormVersion
                .getFormId();

        // Check if user is global admin or has edit permission for the parent form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionEdit,
                    PermissionLabels.FormPermissionEdit);
        }

        // Create the form version
        var createdFormVersion = formVersionService
                .create(newFormVersion);

        // Create the first revision for the form version
        var id = new VFormVersionWithDetailsEntityId(formId, createdFormVersion.getVersion());
        var createdFormVersionDetails = vFormVersionWithDetailsService
                .retrieve(id)
                .orElseThrow(ResponseException::internalServerError);
        formRevisionService
                .create(user, createdFormVersionDetails);

        // Log the form version creation
        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Create, FormVersionEntity.class, Map.of(
                "formId", createdFormVersion.getFormId(),
                "formVersion", createdFormVersion.getVersion()
        )));

        return createdFormVersion;
    }

    @GetMapping("{formId}/latest/")
    @Operation(
            summary = "Retrieve Latest Form Version",
            description = "Retrieve the latest version of a form. " +
                          "Requires read permissions on the parent form unless the user is a super admin."
    )
    public FormVersionEntity retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        // Get the latest version number for the form
        var latestVersion = formVersionService
                .getLatestVersion(formId)
                .orElseThrow(ResponseException::notFound)
                .getVersion();

        return retrieveVersion(jwt, formId, latestVersion);
    }

    @GetMapping("{formId}/{version}/")
    @Operation(
            summary = "Retrieve Specific Form Version",
            description = "Retrieve a specific version of a form. " +
                          "Requires read permissions on the parent form unless the user is a super admin."
    )
    public FormVersionEntity retrieveVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Determine the user for permission checks
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the form by retrieving the form first and checking permissions
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead);
        }

        // Fetch the form version by its id
        var id = FormVersionEntityId
                .of(formId, version);
        return formVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @PutMapping("{formId}/{version}/")
    @Operation(
            summary = "Update Form Version",
            description = "Update a specific version of a form. " +
                          "Requires the system role super admin or edit permissions on the parent form."
    )
    public FormVersionEntity update(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version,
            @Nonnull @Valid @RequestBody FormVersionEntity patchedFormVersion
    ) throws ResponseException {
        // Determine the user to check permissions
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if user is global admin or has edit permission for the parent form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionEdit,
                    PermissionLabels.FormPermissionEdit);
        }

        // Check if the form is locked by another user
        formLockService
                .checkFormLock(formId, user);

        // Fetch the existing form
        var formVersionEntityId = new VFormVersionWithDetailsEntityId(formId, version);
        var existingFormVersionWithDetails = vFormVersionWithDetailsService
                .retrieve(formVersionEntityId)
                .orElseThrow(ResponseException::notFound);

        // Create a lock for the form
        formLockService
                .create(new FormLockCacheEntity()
                        .setFormId(formId)
                        .setUserId(user.getId()));

        // Recalculate referenced IDs for all elements in the form
        ElementStreamUtils
                .applyAction(patchedFormVersion.getRootElement(), BaseElement::recalculateReferencedIds);

        // Update the form version
        var updatedFormVersion = formVersionService
                .update(FormVersionEntityId.of(formId, version), patchedFormVersion);

        // Log the form version update
        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", updatedFormVersion.getFormId(),
                "formVersion", updatedFormVersion.getVersion()
        )));

        // Create a revision for the form
        var updatedFormVersionWithDetails = vFormVersionWithDetailsService
                .retrieve(formVersionEntityId)
                .orElseThrow(ResponseException::notFound);
        formRevisionService
                .create(user, updatedFormVersionWithDetails, existingFormVersionWithDetails);

        // Return the form as a DTO
        return updatedFormVersion;
    }

    @DeleteMapping("{formId}/{version}/")
    @Operation(
            summary = "Delete Form Version",
            description = "Delete a specific version of a form. " +
                          "Requires the system role super admin or edit permissions on the parent form. " +
                          "Only draft versions can be deleted, and the last remaining version cannot be deleted."
    )
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Determine the user to check permissions
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the form by retrieving the form first and checking permissions
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionEdit,
                    PermissionLabels.FormPermissionEdit);
        }

        // Check if the form is currently locked by another user
        formLockService
                .checkFormLock(formId, user);

        var id = FormVersionEntityId
                .of(formId, version);

        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);

        var formVersion = formVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (form.getVersionCount() <= 1) {
            throw ResponseException.conflict("Die letzte Version eines Formulars kann nicht gelöscht werden.");
        }

        if (formVersion.getStatus() != FormStatus.Drafted) {
            throw ResponseException.conflict("Nur Entwurfs-Versionen können gelöscht werden.");
        }

        formVersionService
                .delete(id);
    }

    @GetMapping("{formId}/{version}/publish-checklist-items/")
    @Operation(
            summary = "Get Form Publish Checklist",
            description = "Get the checklist items for publishing a form version. " +
                          "Requires read permissions on the parent form unless the user is a super admin."
    )
    public List<FormPublishChecklistItem> checkPublish(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Determine the user for permission checks
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the form by retrieving the form first and checking permissions
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead);
        }

        var id = new FormVersionEntityId(formId, version);

        var form = formVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        return formVersionService
                .getFormPublishChecklist(form);
    }

    @PutMapping("{formId}/{version}/publish-status/")
    @Operation(
            summary = "Publish Form Version",
            description = "Publish a specific version of a form. " +
                          "Requires the system role super admin or publish permissions on the parent form. " +
                          "If the form is locked by another user, an exception is thrown."
    )
    public FormVersionEntity publish(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Extract staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has the permission to revoke the form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionPublish,
                    PermissionLabels.ProcessPermissionPublish);
        }

        // Check if the form is locked by another user
        formLockService
                .checkFormLock(formId, user);

        var id = new FormVersionEntityId(formId, version);

        // Fetch the existing form
        var formVersionEntityId = new VFormVersionWithDetailsEntityId(formId, version);
        var existingFormVersionWithDetails = vFormVersionWithDetailsService
                .retrieve(formVersionEntityId)
                .orElseThrow(ResponseException::notFound);

        // Publish the form
        var publishedFormVersion = formVersionService
                .publish(id);

        // Log the form publication
        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(
                user,
                AuditAction.Update,
                FormVersionEntity.class,
                Map.of(
                        "formId", publishedFormVersion.getFormId(),
                        "formVersion", publishedFormVersion.getVersion(),
                        "published", true
                )
        ));

        /*
        // Send a message about the form publication
        try {
            formMailService.sendPublished(user, form);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload
                    .create()
                    .setTriggeringUser(user)
                    .setActionType("Exception")
                    .setSeverity("error")
                    .setActionResult("failure")
                    .setReason(e.getMessage())
                    .setMessage("Failed to send message about form publication")
                    .setMetadata(Map.of(
                            "exceptionType", e.getClass().getName(),
                            "formId", form.getId(),
                            "formSlug", form.getSlug(),
                            "formVersion", form.getVersion(),
                            "developingDepartmentId", form.getDevelopingDepartmentId()
                    )));
            exceptionMailService.send(e);
        }
         */

        // Create a revision for the form
        var updatedFormVersionWithDetails = vFormVersionWithDetailsService
                .retrieve(formVersionEntityId)
                .orElseThrow(ResponseException::notFound);
        formRevisionService
                .create(user, updatedFormVersionWithDetails, existingFormVersionWithDetails);

        // Return the form as a DTO
        return publishedFormVersion;
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
    @PutMapping("{formId}/{version}/revoke-status/")
    @Operation(
            summary = "Revoke Form Version",
            description = "Revoke a specific version of a form. " +
                          "Requires the system role super admin or publish permissions on the parent form. " +
                          "If the form is locked by another user, an exception is thrown."
    )
    public FormVersionEntity revoke(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Extract staff user
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has the permission to revoke the form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionPublish,
                    PermissionLabels.FormPermissionPublish);
        }

        // Check if the form is locked by another user
        formLockService
                .checkFormLock(formId, user);

        var id = new FormVersionEntityId(formId, version);

        // Fetch the existing form
        var formVersionEntityId = new VFormVersionWithDetailsEntityId(formId, version);
        var existingFormVersionWithDetails = vFormVersionWithDetailsService
                .retrieve(formVersionEntityId)
                .orElseThrow(ResponseException::notFound);

        // Publish the form
        var revokedFormVersion = formVersionService
                .revoke(id);

        // Log the form publication
        auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload.ofLegacyAction(
                user,
                AuditAction.Update,
                FormVersionEntity.class,
                Map.of(
                        "formId", revokedFormVersion.getFormId(),
                        "formVersion", revokedFormVersion.getVersion(),
                        "published", false
                )
        ));

        /*
        // Send a message about the form publication
        try {
            formMailService.sendPublished(user, form);
        } catch (MessagingException | IOException | NoValidUserEMailsInDepartmentException e) {
            auditService.addAuditEntry(de.aivot.GoverBackend.audit.models.AuditLogPayload
                    .create()
                    .setTriggeringUser(user)
                    .setActionType("Exception")
                    .setSeverity("error")
                    .setActionResult("failure")
                    .setReason(e.getMessage())
                    .setMessage("Failed to send message about form publication")
                    .setMetadata(Map.of(
                            "exceptionType", e.getClass().getName(),
                            "formId", form.getId(),
                            "formSlug", form.getSlug(),
                            "formVersion", form.getVersion(),
                            "developingDepartmentId", form.getDevelopingDepartmentId()
                    )));
            exceptionMailService.send(e);
        }
         */

        // Create a revision for the form
        var updatedFormVersionWithDetails = vFormVersionWithDetailsService
                .retrieve(formVersionEntityId)
                .orElseThrow(ResponseException::notFound);
        formRevisionService
                .create(user, updatedFormVersionWithDetails, existingFormVersionWithDetails);

        // Return the form as a DTO
        return revokedFormVersion;
    }
}
