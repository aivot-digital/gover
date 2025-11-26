package de.aivot.GoverBackend.form.controllers;

import de.aivot.GoverBackend.department.services.DepartmentMembershipService;
import de.aivot.GoverBackend.form.dtos.FormDetailsResponseDTO;
import de.aivot.GoverBackend.form.entities.FormVersionEntityId;
import de.aivot.GoverBackend.form.enums.FormStatus;
import de.aivot.GoverBackend.form.filters.FormVersionWithMembershipFilter;
import de.aivot.GoverBackend.form.services.FormLockService;
import de.aivot.GoverBackend.form.services.FormService;
import de.aivot.GoverBackend.form.services.FormVersionService;
import de.aivot.GoverBackend.form.services.FormVersionWithMembershipService;
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
public class FormVersionController {
    private final FormVersionWithMembershipService formVersionWithMembershipService;
    private final FormVersionService formVersionService;
    private final FormService formService;
    private final DepartmentMembershipService departmentMembershipService;
    private final FormLockService formLockService;

    @Autowired
    public FormVersionController(FormVersionWithMembershipService formVersionWithMembershipService, FormVersionService formVersionService, FormService formService, DepartmentMembershipService departmentMembershipService, FormLockService formLockService) {
        this.formVersionWithMembershipService = formVersionWithMembershipService;
        this.formVersionService = formVersionService;
        this.formService = formService;
        this.departmentMembershipService = departmentMembershipService;
        this.formLockService = formLockService;
    }

    @GetMapping("/api/form-versions/")
    public Page<FormDetailsResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @Valid FormVersionWithMembershipFilter filter
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user is not a global admin
        if (filter.getUserId() == null) {
            filter.setUserId(user.getId());
        }

        return formVersionWithMembershipService
                .list(pageable, filter)
                .map(FormDetailsResponseDTO::fromEntity);
    }

    @GetMapping("/api/forms/{formId}/versions/")
    public Page<FormDetailsResponseDTO> list(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PageableDefault Pageable pageable,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @Valid FormVersionWithMembershipFilter filter
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the form by retrieving the form first and checking permissions
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);
        FormController
                .checkUserHasAccessToForm(user, form.getDevelopingDepartmentId(), departmentMembershipService);

        // Set the user ID to prevent duplicate records
        filter.setUserId(user.getId());

        // Set the form ID in the filter to only get versions for the specified form
        filter.setFormId(form.getId());

        return formVersionWithMembershipService
                .list(pageable, filter)
                .map(FormDetailsResponseDTO::fromEntity);
    }

    @DeleteMapping("/api/forms/{formId}/versions/{versionId}/")
    public void delete(
            @Nullable @AuthenticationPrincipal Jwt jwt,
            @Nonnull @PathVariable Integer formId,
            @Nonnull @PathVariable Integer versionId
    ) throws ResponseException {
        // Check if the user is a staff user
        var user = UserService
                .fromJWT(jwt)
                .orElseThrow(ResponseException::unauthorized);

        // Check if the user has access to the form by retrieving the form first and checking permissions
        var form = formService
                .retrieve(formId)
                .orElseThrow(ResponseException::notFound);
        FormController
                .checkUserHasAccessToForm(user, form.getDevelopingDepartmentId(), departmentMembershipService);
        // Check if the form is currently locked by another user
        FormController
                .checkFormLock(formId, user, formLockService);

        var id = FormVersionEntityId
                .of(formId, versionId);

        var version = formVersionService
                .retrieve(id)
                .orElseThrow(ResponseException::notFound);

        if (form.getVersionCount() <= 1) {
            throw ResponseException.conflict("Die letzte Version eines Formulars kann nicht gelöscht werden.");
        }

        if (version.getStatus() != FormStatus.Drafted) {
            throw ResponseException.conflict("Nur Entwurfs-Versionen können gelöscht werden.");
        }

        formVersionService
                .delete(id);
    }
}
