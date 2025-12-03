package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.audit.enums.AuditAction;
import de.aivot.GoverBackend.audit.services.AuditService;
import de.aivot.GoverBackend.audit.services.ScopedAuditService;
import de.aivot.GoverBackend.form.cache.entities.FormLockCacheEntity;
import de.aivot.GoverBackend.form.entities.*;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormRevisionService;
import de.aivot.GoverBackend.form.services.VFormVersionWithDetailsService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionsService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.openApi.OpenAPIConfiguration;
import de.aivot.GoverBackend.user.services.UserService;
import de.aivot.GoverBackend.userRoles.data.PermissionLabels;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.Map;

@RestController
@RequestMapping("/api/forms/{formId}/{version}/revisions/")
@Tag(
        name = "Form Version Revisions",
        description = "Form revisions track the changes made to form versions over time. " +
                      "They allow administrators to view the history of modifications and revert to previous states if necessary."
)
@SecurityRequirement(name = OpenAPIConfiguration.Name)
public class FormRevisionController {
    private final ScopedAuditService auditService;

    private final FormLockService formLockService;
    private final FormRevisionService formRevisionService;
    private final VFormWithPermissionsService vFormWithPermissionsService;
    private final VFormVersionWithDetailsService vFormVersionWithDetailsService;
    private final UserService userService;

    @Autowired
    public FormRevisionController(AuditService auditService,
                                  FormLockService formLockService,
                                  FormRevisionService formRevisionService,
                                  VFormWithPermissionsService vFormWithPermissionsService,
                                  VFormVersionWithDetailsService vFormVersionWithDetailsService,
                                  UserService userService) {
        this.auditService = auditService.createScopedAuditService(FormRevisionController.class);

        this.formLockService = formLockService;
        this.formRevisionService = formRevisionService;
        this.vFormWithPermissionsService = vFormWithPermissionsService;
        this.vFormVersionWithDetailsService = vFormVersionWithDetailsService;
        this.userService = userService;
    }

    @GetMapping("")
    @Operation(
            summary = "List form revisions",
            description = "Retrieve a paginated list of revisions for a specific form version." +
                          "Requires \"" + PermissionLabels.FormPermissionRead + "\" permission unless the user is a global admin."
    )
    public Page<FormRevisionEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @ParameterObject @PageableDefault Pageable pageable,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Determine the user for the permission check
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has the permission to revoke the form
        if (!user.getIsSuperAdmin()) {
            vFormWithPermissionsService.checkUserPermission(
                    formId,
                    user.getId(),
                    VFormWithPermissionsEntity::getFormPermissionRead,
                    PermissionLabels.FormPermissionRead
            );
        }

        // Return the paginated list of form revisions
        return formRevisionService
                .list(formId, version, pageable);
    }

    @GetMapping("rollback/{revisionId}/")
    @Operation(
            summary = "Rollback form to revision",
            description = "Rollback a specific form version to a previous revision." +
                          "Requires \"" + PermissionLabels.FormPermissionEdit + "\" permission unless the user is a global admin."
    )
    public VFormVersionWithDetailsEntity rollback(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer formId,
            @PathVariable Integer version,
            @PathVariable BigInteger revisionId
    ) throws ResponseException {
        var user = userService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has the permission to revoke the form
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

        // Create a form lock for the current user because this operation might take some time
        formLockService.create(new FormLockCacheEntity()
                .setFormId(formId)
                .setUserId(user.getId()));

        var formVersionEntity = vFormVersionWithDetailsService
                .retrieve(new VFormVersionWithDetailsEntityId(formId, version))
                .orElseThrow(ResponseException::notFound);

        var rolledBackForm = formRevisionService
                .rollback(formVersionEntity, revisionId);

        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", rolledBackForm.getId(),
                "formSlug", rolledBackForm.getSlug(),
                "developingDepartmentId", rolledBackForm.getDevelopingDepartmentId()
        ));

        auditService.logAction(user, AuditAction.Update, FormEntity.class, Map.of(
                "formId", rolledBackForm.getFormId(),
                "formVersion", rolledBackForm.getVersion()
        ));

        // Create a revision for the form
        formRevisionService
                .create(user, rolledBackForm, formVersionEntity);

        return rolledBackForm;
    }
}
