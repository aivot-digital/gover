package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.form.entities.FormVersionEntity;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.entities.VFormWithPermissionEntity;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.FormVersionFilter;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.VFormWithPermissionService;
import de.aivot.GoverBackend.lib.exceptions.ResponseException;
import de.aivot.GoverBackend.user.services.UserService;
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

@RestController
@RequestMapping("/api/form-versions/")
public class FormVersionController {
    private final FormVersionService formVersionService;
    private final FormService formService;
    private final FormLockService formLockService;
    private final VFormWithPermissionService vFormWithPermissionService;

    @Autowired
    public FormVersionController(FormVersionService formVersionService,
                                 FormService formService,
                                 FormLockService formLockService,
                                 VFormWithPermissionService vFormWithPermissionService) {
        this.formVersionService = formVersionService;
        this.formService = formService;
        this.formLockService = formLockService;
        this.vFormWithPermissionService = vFormWithPermissionService;
    }

    @GetMapping("")
    public Page<FormVersionEntity> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormVersionFilter filter
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        return formVersionService
                .list(pageable, filter);
    }

    @PostMapping("")
    public FormVersionEntity create(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @Valid @RequestBody FormVersionEntity newFormVersion
    ) throws ResponseException {
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var formId = newFormVersion
                .getFormId();

        var canEditForm = user.getGlobalAdmin() || vFormWithPermissionService
                .checkUserPermission(formId, user.getId(), VFormWithPermissionEntity::getFormPermissionEdit);
        if (!canEditForm) {
            throw ResponseException.noPermission("Formulare Bearbeiten");
        }

        return formVersionService
                .create(newFormVersion);
    }

    @GetMapping("{formId}/latest/")
    public FormVersionEntity retrieveLatest(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var latestVersion = formVersionService
                .getLatestVersion(formId)
                .orElseThrow(ResponseException::notFound)
                .getVersion();

        return retrieveVersion(jwt, formId, latestVersion);
    }

    @GetMapping("{formId}/{version}/")
    public FormVersionEntity retrieveVersion(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        var id = FormVersionEntityId
                .of(formId, version);

        return formVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);
    }

    @DeleteMapping("{formId}/{version}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer version
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the form by retrieving the form first and checking permissions
        var canEditForm = user.getGlobalAdmin() || vFormWithPermissionService
                .checkUserPermission(formId, user.getId(), VFormWithPermissionEntity::getFormPermissionEdit);
        if (!canEditForm) {
            throw ResponseException.noPermission("Formulare Bearbeiten");
        }

        // Check if the form is currently locked by another user
        FormController
                .checkFormLock(formId, user, formLockService);

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
}
